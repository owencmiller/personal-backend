(ns game-backend.handler
  (:require [aleph.http :as http]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [game-backend.games.noname :as nn]
            [jumblerg.middleware.cors :refer [wrap-cors]]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [nrepl.server :as nrepl-server]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [aleph.netty :as netty]))



(def initial-state {:lobby {}
                    :games {}})

(def conns (atom initial-state))


(defn get-game
  [game-id]
  (get-in @conns [:games game-id]))

(defn send-message
  [conn message-map]
  (prn "sending message: " message-map)
  (s/put! conn (pr-str message-map)))

(defn create-player
  [conn]
  {:id (java.util.UUID/randomUUID)
   :in-game? nil
   :is-host? false
   :game-id nil
   :conn conn})

(defn into-map
  ([kf coll]
   (into-map kf identity coll))
  ([kf vf coll]
   (into {} (map (fn [el] [(kf el) (vf el)]) coll))))

(defn get-player
  [id]
  (or (get (:lobby @conns) id)
      (get (->> (:games @conns)
                vals
                (mapcat :players)
                (into {}))
           id)))

(defn get-game-players
  [game-id]
  (->> (get-game game-id)
       :players
       vals))

(defn get-host
  [game-id]
  (get-player (get-in @conns [:games game-id :host])))

(defn broadcast
  [game-id message]
  (prn "broadcasting to: " (map :id (get-game-players game-id)))
  (doseq [conn (map :conn (get-game-players game-id))]
    (send-message conn message)))



(def game-id-max-chars 3)
(def all-chars (str/split "ABCDEFGHIJKLMNOPQRSTUVWXYZ" #""))
(defn create-game-id
  ([]
   (create-game-id all-chars game-id-max-chars))
  ([coll n]
   (->> (repeatedly n #(rand-nth coll))
        (apply str))))



(defn create-new-game
  [state host-id]
  (let [host (get (:lobby state) host-id)
        game-id (create-game-id)]
    (send-message (:conn host) {:game-id game-id
                                :in-game? true
                                :is-host? true
                                :game-state nn/game-start-state})
    (-> state
        (update-in [:lobby] dissoc host-id)
        (update-in [:games] assoc game-id {:host host-id
                                           :players {host-id host}
                                           :game-state nn/game-start-state}))))


(defn join-game
  [state id game-id]
  (let [player (get (:lobby state) id)
        game-host (get-host game-id)
        game-state (get-in @conns [:games game-id :game-state])
        new-game-state (nn/add-player game-state id)]
    (prn "joinging a game with host: " game-host)
    (send-message (:conn player) {:game-id game-id
                                  :in-game? true
                                  :is-host? false})
    (send-message (:conn game-host)
                  {:game-state new-game-state})
    (-> state
        (update-in [:lobby] dissoc id)
        (update-in [:games game-id :players] assoc id player)
        (assoc-in [:games game-id :game-state] new-game-state))))


(defn process-move
  [state id {:keys [game-id delta]}]
  (prn "processing move - " game-id)
  (let [game-state (get-in @conns [:games game-id :game-state])
        _ (prn "game-state" game-state)
        new-game-state (nn/process-move game-state id delta)
        _ (prn "new-game-state" new-game-state)]
    (prn new-game-state)
    (broadcast game-id {:game-state new-game-state})
    (-> state
        (assoc-in [:games game-id :game-state] new-game-state))))


(defn process-message
  [message]
  (prn "recieved message: " message)
  (let [{:keys [id command data]} message]
    (case command
      :host-game (swap! conns create-new-game id)
      :join-game (swap! conns join-game id (:game-id data))
      :make-move (swap! conns process-move id data))))











(defn socket-handler
  [socket]
  (let [new-player (create-player socket)]
    (swap! conns assoc-in [:lobby (:id new-player)] new-player)
    (s/put! socket (pr-str (dissoc new-player :conn)))

    ;; Listen for messages
    (while true
      (let [message @(s/take! socket)]
        (process-message (read-string message))))))


(def non-websocket-request
  {:status 400
   :headers {"content-type" "application/text"}
   :body "Expected a websocket request."})

(defn echo-handler
  [req]
  (-> (http/websocket-connection req)
      (d/chain
       socket-handler)
      (d/catch
       (fn [_]
         non-websocket-request))))


(defroutes app-routes
  (GET "/connect" [] echo-handler)
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      (wrap-defaults site-defaults)
      (wrap-cors #".*")))



;; Server management

(defonce nrepl (nrepl-server/start-server :port 7000))
(defonce server (atom nil))

(defn start-server []
  (prn "Starting secure server on port 443...")
  (let [ssl-context (netty/ssl-server-context
                     {:certificate-chain (io/file "/etc/letsencrypt/live/api.owenmiller.me/fullchain.pem")
                      :private-key       (io/file "/etc/letsencrypt/live/api.owenmiller.me/privkey.pem")})]
    (reset! server
            (http/start-server app
                               {:port        3001
                                :ssl-context ssl-context}))))

(defn stop-server
  []
  (prn "Stopping server...")
  (reset! conns initial-state)
  (.close @server)
  (reset! server nil))

(defn restart-server
  []
  (stop-server)
  (start-server))

(defn -main []
  (start-server))


(comment
  (restart-server))