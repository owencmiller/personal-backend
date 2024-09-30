(ns game-backend.games.tetris)

(def HEIGHT 5)
(def WIDTH 5)
(def initial-board (vec (repeat HEIGHT (vec (repeat WIDTH nil)))))
(def tetronimoes [[[]]])

(def initial-game-state
  {:board initial-board
   :piece-falling? false})


;; __ __ __ __ __
;; __ __ __ __ __
;; __ __ __ __ __
;; __ __ __ __ __
;; __ __ __ __ __
