(ns game-backend.games.tic-tac-toe)

(def status-vals #{:playing :x-won :o-won :tied})

(def winning-pos
  [[0 1 2]
   [3 4 5]
   [6 7 8]
   [0 3 6]
   [1 4 7]
   [2 5 8]
   [0 4 8]
   [6 4 2]])

(def initial-state
  {:board (vec (repeat 9 nil))
   :players [:x :o]
   :current-player :x
   :turn-count 0
   :status :playing})

(defn eval-board
  [{:keys [board current-player status] :as state}]
  (some identity (map (fn [win-pos]
                        (when (apply = (map #(nth board %) win-pos))
                          (nth board (first win-pos))))
                      winning-pos)))

(defn make-move
  [{:keys [board current-player status] :as state} player move]
  (when (and (not (nth board move))
             (= current-player player)
             (= status :playing))
    (let [new-state (-> state
                        (update :board assoc move player)
                        (update :turn-count inc)
                        (update :current-player #(if (= % :x) :o :x)))
          winner? (eval-board new-state)]
      (if winner?
        (assoc new-state :status winner?)
        new-state))))



(-> (make-move initial-state :x 0)
    (make-move :o 8)
    (make-move :x 1)
    (make-move :o 7)
    (make-move :x 2))