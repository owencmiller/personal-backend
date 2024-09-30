(ns game-backend.games.jane-street)

(defn avg-list [l]
  (float (/ (apply + l) (count l))))

(defn roll-die [acc]
  (assoc acc :d (inc (rand-int 20))))

(defn take-money [{:keys [d] :as acc}]
  (update acc :score + d))

(defn strat1 [acc round]
  (if (odd? round)
    (roll-die acc)
    (take-money acc)))

(defn strat2 [acc round]
  (if (> (:d acc) 18)
    (take-money acc)
    (roll-die acc)))

(defn strat-builder [n]
  (fn strat [acc round]
    (if (> (:d acc) n)
      (-> (take-money acc)
          (assoc :r round))
      (roll-die acc))))

(defn run [s]
  (fn [] (reduce s {:score 0 :d 1} (range 1 101))))


#_(defn game-avg [strat]
    (->> (repeatedly 100 (run strat))
         (map #(update))))

(comment
  #_(->> (map strat-builder (range 1 21))
         (map game-avg)))