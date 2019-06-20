(ns figma.core
  (:require [clj-http.client :as http]
            [cheshire.core :as c]
            [clojure.spec.alpha :as s]
            [rum.core :as rum]
            [zprint.core :as zp]
            [figma.hiccup :as hiccup]
            [figma.specs]
            [clojure.string :as str]))

(defn key->file-url [key]
  (str "https://api.figma.com/v1/files/" key))

(defn key->image-url [key {:keys [ids format]}]
  (str "https://api.figma.com/v1/images/" key
       "?ids=" (str/join "," ids)
       "&format=" (name format)))

(defn get-figma-file [token key]
  (-> (http/get (key->file-url key) {:headers {"X-Figma-Token" token}})
      :body
      (c/parse-string true)))

(defn get-figma-images [token key ids]
  (-> (http/get (key->image-url key {:ids ids :format :svg})
                {:headers {"X-Figma-Token" token}})
      :body
      (c/parse-string true)
      :images))

;; ==============================================

(comment
  (require '[figma.specs] :reload-all)
  (require '[figma.hiccup] :reload-all)
  (require '[garden.core :refer [css]])

  (def f-file
    (get-figma-file
      'Personal-Access-Tokens
      'Document-ID))

  (binding [figma.hiccup/*css-cache* (atom #{})]
    (let [{:keys [schemaVersion document]} f-file
          ast         (->> (:document f-file)
                           (s/conform :figma/node))
          hiccup      (hiccup/figma->hiccup ast)
          styles      (-> @figma.hiccup/*css-cache*
                          (clojure.string/join))
          html        (rum/render-static-markup hiccup)
          {figma-document :document
           components     :components}
          (hiccup/hiccup->components hiccup "components")
          base-styles (css [:body {:margin 0}]
                           [:.figma-canvas {:min-height "100vh"
                                            :min-width  "100vw"}])
          pre         (str "<style>" base-styles "</style>"
                           "<style>" styles "</style>")]
      (->> (str pre html)
           (spit "components.html"))
      (-> (conj components '(ns figma.components
                              (:require [reagent.core :as r])))
          (->> (into []))
          (conj `(~'defn ~'figma-document [] ~figma-document)
                `(~'r/render [~'figma-document] (.-body js/document)))
          (->> (map zp/zprint-str)
               (interpose "\n\n")
               (apply str)
               (spit "src/figma/components.cljs"))))))
