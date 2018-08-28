(ns figma.ui)

(defn -hiccup->rum [[cname hiccup]]
  `(~'rum/defc ~(symbol cname) ~'<
     ~'rum/static
     []
     ~hiccup))

(defn hiccup->rum [components]
  (map -hiccup->rum components))
