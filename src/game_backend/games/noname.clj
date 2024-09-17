(ns game-backend.games.noname)

(def game-start-state
  {})

(def colors ["red" "blue" "green"])
(defn random-color
  []
  (nth colors (rand-int (count colors))))

(defn add-player
  [game-state player-id]
  (-> game-state
      (assoc player-id {:pos 0 :color (random-color)})))


(defn process-move
  [game-state player-id move]
  (-> game-state
      (update-in [player-id :pos] + move)))