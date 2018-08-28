(ns figma.core
  (:require [clj-http.client :as http]
            [cheshire.core :as c]
            [clojure.spec.alpha :as s]
            [rum.core :as rum]
            [zprint.core :as zp]
            [figma.hiccup :as hiccup]
            [figma.ui :as ui]
            [figma.specs]))

(defn key->url [key]
  (str "https://api.figma.com/v1/files/" key))

(defn get-figma-file [token key]
  (-> (http/get (key->url key) {:headers {"X-Figma-Token" token}})
      :body
      (c/parse-string true)))

;; ==============================================

(comment
  (require '[figma.specs] :reload-all)
  (require '[figma.hiccup] :reload-all)
  (require '[figma.ui :as ui] :reload-all)
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
                              (:require [rum.core :as rum])))
          (->> (into []))
          (conj `(~'rum/defc ~'figma-document [] ~figma-document)
                `(~'rum/mount (~'figma-document) (.-body js/document)))
          (->> (map zp/zprint-str)
               (interpose "\n\n")
               (apply str)
               (spit "src/figma/components.cljs"))))))
