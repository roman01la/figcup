(ns figma.specs
  (:require [clojure.spec.alpha :as s]))

(def color-chan
  (s/and number? #(<= 0 % 1)))

(def alpha-chan
  (s/and number? #(<= 0 % 1)))

;;=============================================
;; File Format Types
;;=============================================

;; Color
(s/def :figma.types.color/r color-chan)
(s/def :figma.types.color/g color-chan)
(s/def :figma.types.color/b color-chan)
(s/def :figma.types.color/a alpha-chan)

(s/def :figma.types/color
  (s/keys :req-un [:figma.types.color/r
                   :figma.types.color/g
                   :figma.types.color/b
                   :figma.types.color/a]))

;; Rectangle
(s/def :figma.types.rect/x int?)
(s/def :figma.types.rect/y int?)
(s/def :figma.types.rect/width int?)
(s/def :figma.types.rect/height int?)

(s/def :figma.types/rect
  (s/keys :req-un [:figma.types.rect/x
                   :figma.types.rect/y
                   :figma.types.rect/width
                   :figma.types.rect/height]))

;; Vector
(s/def :figma.types.vector/x int?)
(s/def :figma.types.vector/y int?)

(s/def :figma.types/vector
  (s/keys :req-un [:figma.types.vector/x
                   :figma.types.vector/y]))

;; ColorStop
(s/def :figma.types.color-stop/position
  (s/and number? #(<= 0 % 1)))

(s/def :figma.types.color-stop/color
  :figma.types/color)

(s/def :figma.types/color-stop
  (s/keys :req-un [:figma.types.color-stop/position
                   :figma.types.color-stop/color]))

;; Effect
(s/def :figma.types.effect/type #{"INNER_SHADOW" "DROP_SHADOW"})
(s/def :figma.types.effect/visible boolean?)
(s/def :figma.types.effect/radius int?)
(s/def :figma.types.effect/color :figma.types/color)
(s/def :figma.types.effect/offset :figma.types/vector)

(s/def :figma.types/effect
  (s/keys :req-un [:figma.types.effect/type
                   :figma.types.effect/visible
                   :figma.types.effect/radius
                   :figma.types.effect/color
                   :figma.types.effect/offset]))

;; Paint
(s/def :figma.types.paint/type #{"SOLID" "GRADIENT_LINEAR"})
(s/def :figma.types.paint/visible boolean?)
(s/def :figma.types.paint/opacity (s/and number? #(<= 0 % 1)))
(s/def :figma.types.paint/color :figma.types/color)

(s/def :figma.types.paint/gradientHandlePositions
  (s/coll-of :figma.types/vector))

(s/def :figma.types.paint/gradientStops
  (s/coll-of :figma.types/color-stop))

(s/def :figma.types/paint
  (s/keys :req-un [:figma.types.paint/type
                   :figma.types.paint/visible
                   :figma.types.paint/opacity
                   :figma.types.paint/color
                   :figma.types.paint/gradientHandlePositions
                   :figma.types.paint/gradientStops]))

;; TypeStyle
(s/def :figma.types.type-style/fontFamily string?)
(s/def :figma.types.type-style/italic boolean?)
(s/def :figma.types.type-style/fontWeight pos-int?)
(s/def :figma.types.type-style/fontSize pos-int?)
(s/def :figma.types.type-style/textAlignHorizontal #{"LEFT" "RIGHT" "CENTER" "JUSTIFIED"})
(s/def :figma.types.type-style/letterSpacing int?)

(s/def :figma.types.type-style/fills
  (s/coll-of :figma.types/paint))

(s/def :figma.types.type-style/lineHeightPx int?)

(s/def :figma.types/type-style
  (s/keys :req-un [:figma.types.type-style/fontFamily
                   :figma.types.type-style/italic
                   :figma.types.type-style/fontWeight
                   :figma.types.type-style/fontSize
                   :figma.types.type-style/textAlignHorizontal
                   :figma.types.type-style/letterSpacing
                   :figma.types.type-style/fills
                   :figma.types.type-style/lineHeightPx]))

;; Component
(s/def :figma.types.component/name string?)
(s/def :figma.types.component/description string?)

(s/def :figma.types/component
  (s/keys :req-un [:figma.types.component/name
                   :figma.types.component/description]))

;;=============================================
;; JSON File Format Types / Node Properties
;;=============================================

(s/def :figma.types/children
  (s/coll-of :figma.nodes/node))

(s/def :figma.types/backgroundColor
  :figma.types/color)

(s/def :figma.types/opacity
  (s/and int? #(<= 0 % 1)))

(s/def :figma.types/absoluteBoundingBox
  :figma.types/rect)

(s/def :figma.types/effects
  (s/coll-of :figma.types/effect))

(s/def :figma.types/fills
  (s/coll-of :figma.types/paint))

(s/def :figma.types/strokes
  (s/coll-of :figma.types/paint))

(s/def :figma.types/strokeWeight pos-int?)
(s/def :figma.types/cornerRadius pos-int?)
(s/def :figma.types/characters string?)
(s/def :figma.types/style :figma.types/type-style)
(s/def :figma.types/componentId string?)

;; Node
(s/def :figma.nodes.node/id string?)
(s/def :figma.nodes.node/name string?)
(s/def :figma.nodes.node/visible boolean?)
(s/def :figma.nodes.node/type
  #{"DOCUMENT" "CANVAS" "FRAME" "GROUP" "VECTOR"
    "ELLIPSE" "RECTANGLE" "TEXT" "COMPONENT" "INSTANCE"})

(s/def :figma.nodes/node
  (s/keys :req-un [:figma.nodes.node/id
                   :figma.nodes.node/name
                   :figma.nodes.node/type]
          :opt-un [:figma.nodes.node/visible]))

;; Document
(s/def :figma.nodes/document
  (s/keys :req-un [:figma.types/children]))

;; Canvas
(s/def :figma.nodes/canvas
  (s/keys :req-un [:figma.types/children
                   :figma.types/backgroundColor]))

;; Frame
(s/def :figma.nodes/frame
  (s/keys :req-un [:figma.types/children
                   :figma.types/backgroundColor
                   :figma.types/absoluteBoundingBox]
          :opt-un [:figma.types/opacity
                   :figma.types/effects]))

;; Group
(s/def :figma.nodes/group
  :figma.nodes/frame)

;; Vector
(s/def :figma.nodes/vector
  (s/keys :req-un [:figma.types/absoluteBoundingBox
                   :figma.types/strokeWeight]
          :opt-un [:figma.types/opacity
                   :figma.types/effects
                   :figma.types/fills
                   :figma.types/strokes]))

;; Rectangle
(s/def :figma.nodes/rect
  (s/merge :figma.nodes/vector
           (s/keys :req-un [:figma.types/cornerRadius])))

;; Ellipse
(s/def :figma.nodes/ellipse
  :figma.nodes/vector)

;; Text
(s/def :figma.nodes/text
  (s/merge :figma.nodes/vector
           (s/keys :req-un [:figma.types/characters
                            :figma.types/style])))

;; Component
(s/def :figma.nodes/component
  :figma.nodes/frame)

;; Instance
(s/def :figma.nodes/instance
  (s/merge :figma.nodes/frame
           (s/keys :req-un [:figma.types/componentId])))

;; Generic Node
(defmulti figma-node :type)

(defmethod figma-node "DOCUMENT" [_]
  :figma.nodes/document)

(defmethod figma-node "CANVAS" [_]
  :figma.nodes/canvas)

(defmethod figma-node "FRAME" [_]
  :figma.nodes/frame)

(defmethod figma-node "GROUP" [_]
  :figma.nodes/group)

(defmethod figma-node "VECTOR" [_]
  :figma.nodes/vector)

(defmethod figma-node "RECTANGLE" [_]
  :figma.nodes/rect)

(defmethod figma-node "ELLIPSE" [_]
  :figma.nodes/ellipse)

(defmethod figma-node "TEXT" [_]
  :figma.nodes/text)

(defmethod figma-node "COMPONENT" [_]
  :figma.nodes/component)

(defmethod figma-node "INSTANCE" [_]
  :figma.nodes/instance)

(s/def :figma/node
  (s/multi-spec figma-node :figma/node))
