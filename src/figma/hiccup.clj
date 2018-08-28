(ns figma.hiccup
  (:require [rum.server-render :as ssr]))

(def css-reset
  {:button {:border-radius 0
            :border        "none"}})

(defn round-to
  ([n]
   (round-to 100 n))
  ([precision n]
   (double (/ (Math/round (double (* precision n))) precision))))

(defn figma-color->rgba [{:keys [r g b a]}]
  (let [v (->> (mapv double [r g b])
               (mapv #(Math/round (* 255 %))))
        v (conj v (round-to a))
        v (->> (interpose ", " v)
               (apply str))]
    (str "rgba(" v ")")))

(defn merge-css [effects]
  (->> (group-by first effects)
       (map (fn [[key values]]
              [key (->> (map second values)
                        (interpose ", ")
                        (apply str))]))
       (into {})))



(defmulti figma-effect->css (fn [_ v] (:type v)))

(defmethod figma-effect->css "INNER_SHADOW"
  [key {:keys [color offset radius]}]
  (let [{:keys [x y]} offset]
    [key (str "inset " x "px " y "px " radius "px " (figma-color->rgba color))]))

(defmethod figma-effect->css "DROP_SHADOW"
  [key {:keys [color offset radius]}]
  (let [{:keys [x y]} offset]
    [key (str x "px " y "px " radius "px " (figma-color->rgba color))]))



(defmulti figma-fill->css (fn [_ v] (:type v)))

(defmethod figma-fill->css "SOLID" [key {:keys [color]}]
  [key (figma-color->rgba color)])

(defmethod figma-fill->css "GRADIENT_LINEAR" [key {:keys [color]}]
  [key (figma-color->rgba color)])



(defn -constraints->helper-attrs [{:keys [vertical horizontal]}]
  {:data-constraint-x horizontal
   :data-constraint-y vertical})



(defmulti -figma-styles->css first)

(defmethod -figma-styles->css :backgroundColor [[_ color]]
  {:background-color (figma-color->rgba color)})

(defmethod -figma-styles->css :opacity [[_ opacity]]
  {:opacity opacity})

(defmethod -figma-styles->css :absoluteBoundingBox [[_ {:keys [width height]}]]
  {:width  width
   :height height})

(defmethod -figma-styles->css :effects [[_ effects]]
  (->> (map #(figma-effect->css :box-shadow %) effects)
       merge-css))

(defmethod -figma-styles->css :fills [[_ fills]]
  (->> (map #(figma-fill->css :background-color %) fills)
       merge-css))

(defmethod -figma-styles->css :strokeWeight [[_ weight]]
  {:border-width weight})

(defmethod -figma-styles->css :strokes [[_ strokes]]
  (if-not (empty? strokes)
    (let [stroke (first strokes)
          stroke (-> stroke
                     (assoc-in [:color :a] (:opacity stroke)))]
      {:border-color (->> stroke
                          (figma-fill->css :color)
                          second)
       :border-style (clojure.string/lower-case (:type stroke))})
    {}))

(defmethod -figma-styles->css :cornerRadius [[_ radius]]
  {:border-radius radius})

(defmethod -figma-styles->css :font-type-style
  [[_ {:keys [fontFamily
              italic
              fontWeight
              fontSize
              textAlignHorizontal
              letterSpacing
              lineHeightPx]}]]
  {:font-family    fontFamily
   :font-weight    fontWeight
   :font-size      (str fontSize "px")
   :text-align     (clojure.string/lower-case textAlignHorizontal)
   :letter-spacing (round-to letterSpacing)
   :line-height    (str lineHeightPx "px")
   :font-style     (when italic "italic")})

(defmethod -figma-styles->css :cornerRadius [[_ radius]]
  {:border-radius radius})

(defmethod -figma-styles->css :text-fills [[_ fills]]
  (->> (map #(figma-fill->css :color %) fills)
       merge-css))

(defmethod -figma-styles->css :text-effects [[_ effects]]
  (->> (map #(figma-effect->css :text-shadow %) effects)
       merge-css))



(defn figma-styles->css [styles]
  (->> (map -figma-styles->css styles)
       (into {})
       (filter second)
       (into {})
       (hash-map :style)))

;; ==========================================

(def ^:dynamic *css-cache*)

(defn -render-css [{:keys [style] :as attrs}]
  (let [class (str "figma-" (hash style))
        sb    (StringBuilder.)]
    (ssr/append! sb "." class "{")
    (doseq [[k v] style]
      (let [k (ssr/normalize-css-key k)
            v (ssr/normalize-css-value k v)]
        (ssr/append! sb k ":" v ";")))
    (ssr/append! sb "}")
    (->> (str sb)
         (swap! *css-cache* conj))
    (-> attrs
        (dissoc :style)
        (update :class #(str % " " class)))))

;; ==========================================

(defmulti figma-node->hiccup :type)

(defmethod figma-node->hiccup "DOCUMENT" [{:keys [children]}]
  `[:div.figma-document {}
    ~@(map figma-node->hiccup children)])

(defmethod figma-node->hiccup "CANVAS"
  [{:keys [children name] :as element}]
  (let [attrs   (select-keys element [:backgroundColor])
        attrs   (-render-css (figma-styles->css attrs))
        element `[:div.figma-canvas ~attrs
                  ~@(map figma-node->hiccup children)]]
    (with-meta element {:figma/type :figma/canvas
                        :figma/name name})))

(defmethod figma-node->hiccup "FRAME"
  [{:keys [children] :as element}]
  (let [constraints (-constraints->helper-attrs (:constraints element))
        attrs       (select-keys element [:backgroundColor
                                          :opacity
                                          :absoluteBoundingBox
                                          :effects])
        attrs       (-> (figma-styles->css attrs)
                        (assoc-in [:style :position] "relative")
                        (merge constraints)
                        -render-css)]
    `[:div.figma-frame ~attrs
      ~@(map figma-node->hiccup children)]))

(defmethod figma-node->hiccup "GROUP" [{:keys [children] :as element}]
  (let [attrs (select-keys element [:backgroundColor
                                    :absoluteBoundingBox
                                    :effects])
        attrs (-render-css (figma-styles->css attrs))]
    `[:div.figma-group ~attrs
      ~@(map figma-node->hiccup children)]))

(defmethod figma-node->hiccup "VECTOR" [{:keys [children] :as element}]
  (let [attrs (select-keys element [:opacity
                                    :absoluteBoundingBox
                                    :effects
                                    :fills
                                    :strokes
                                    :strokeWeight])
        attrs (-render-css (figma-styles->css attrs))]
    `[:div.figma-vector ~attrs
      ~@(map figma-node->hiccup children)]))

(defmethod figma-node->hiccup "RECTANGLE" [element]
  (let [attrs              (select-keys element [:opacity
                                                 :absoluteBoundingBox
                                                 :effects
                                                 :fills
                                                 :strokes
                                                 :strokeWeight
                                                 :cornerRadius
                                                 :constraints])
        tag-name           (some->> (:name element)
                                    (re-find #"\[(.*)\]")
                                    second
                                    keyword)
        tag-name           (or tag-name :div)
        default-tag-styles (get css-reset tag-name nil)
        styles             (merge default-tag-styles
                                  (:style (figma-styles->css attrs)))
        attrs              (-render-css {:class "figma-rect" :style styles})]
    [tag-name attrs]))

(defmethod figma-node->hiccup "ELLIPSE" [element]
  (->> (assoc element :type "RECTANGLE" :cornerRadius "50%")
       figma-node->hiccup))

(defmethod figma-node->hiccup "TEXT"
  [{:keys [characters style opacity effects fills]}]
  (let [attrs (figma-styles->css {:opacity         opacity
                                  :font-type-style style
                                  :text-effects    effects
                                  :text-fills      fills})
        attrs (-render-css attrs)]
    [:div.figma-text attrs characters]))

(defmethod figma-node->hiccup "COMPONENT" [{:keys [name] :as element}]
  (let [element (->> (assoc element :type "FRAME")
                     figma-node->hiccup)]
    (with-meta element {:figma/type :figma/component
                        :figma/name name})))

(defmethod figma-node->hiccup "INSTANCE" [{:keys [name] :as element}]
  (let [element (->> (assoc element :type "FRAME")
                     figma-node->hiccup)]
    (with-meta element {:figma/type :figma/instance
                        :figma/name name})))


(defn figma->hiccup [document]
  (figma-node->hiccup document))


;; ==========================================

(def components* (atom '()))

(defn -hiccup->components [element cc]
  (if (or (string? element) (number? element))
    element
    (let [[tag attrs & children] element
          {:figma/keys [type name]} (meta element)]
      (cond
        (and (= type :figma/canvas) (= name cc))
        (doseq [element children]
          (-hiccup->components element cc))

        (= type :figma/component)
        (let [component `(~'rum/defc ~(symbol name) []
                           [~tag ~attrs ~@(map #(-hiccup->components % cc) children)])]
          (swap! components* conj component)
          nil)

        (= type :figma/instance)
        (list (symbol name))

        (not (empty? children))
        `[~tag ~attrs ~@(map #(-hiccup->components % cc) children)]

        :else
        [tag attrs]))))

(defn hiccup->components [element cc]
  (reset! components* '())
  (let [document (-hiccup->components element cc)]
    {:document   document
     :components @components*}))
