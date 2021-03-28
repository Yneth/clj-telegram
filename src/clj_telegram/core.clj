(ns clj-telegram.core
  (:require [clojure.string :as cstr]
            [clj-http.client :as http]
            [cheshire.core :as cheshire]))

(def api-url "https://api.telegram.org/bot")

; investigate possibility of monkey patching http-client
; using https://github.com/clojure-goes-fast/lazy-require
(defn- request [token action data & [{:keys [multipart?]}]]
  (let [url      (str api-url token "/" action)
        body     (if multipart?
                   (let [multipart-data (map (fn [[n v]] {:name n :content v}) data)]
                     {:multipart multipart-data})
                   {:form-params data
                    :as          :text})
        response (http/post url body)
        json     (cheshire/parse-string (:body response) keyword)]
    json))

(defn get-me [token]
  (request token "getMe" nil))

(defn ->snake-case [obj]
  (into
    {}
    (map (fn [[k v]] [(cstr/replace (name k) #"-" "_") v]))
    obj))

(defn send-message [token chat-id text
                    & [{:keys [parse_mode
                               entities
                               disable-web-page-preview
                               disable-notification
                               reply-to-message-id
                               allow-sending-without-reply
                               reply-markup]
                        :as   opts}]]
  (request
    token
    "sendMessage"
    (->snake-case
      (merge
        {:chat_id chat-id
         :text    text}
        opts)))
  true)

(defn send-photo-file [token chat-id photo
                       & [{:keys [caption
                                  parse-mode
                                  caption-entities
                                  disable-notification
                                  reply-to-message-id
                                  allow-sending-without-reply
                                  reply-markup]
                           :as   opts}]]
  (request
    token
    "sendPhoto"
    (->snake-case
      (merge
        {:chat_id chat-id
         :photo   photo}
        opts))
    {:multipart? true}))

(defn delete-webhook [token & [drop-pending-updates]]
  (let [drop-pending-updates (or drop-pending-updates false)]
    (request
      token
      "deleteWebhook"
      {:drop_pending_updates drop-pending-updates})))

(defn get-webhook-info [token]
  (request token "getWebhookInfo" nil))

; https://core.telegram.org/bots/api#setwebhook
(defn set-webhook [token
                   {:keys [url
                           certificate
                           ip_address
                           max_connections
                           allowed_updates
                           drop_pending_updates]
                    :as   data}]
  (request token "setWebhook" data))

(defn get-updates
  ([token] (request token "getUpdates" nil))
  ([token offset] (request token "getUpdates" {:offset offset :limit 1})))

(defn process-update [token update commands]
  (let [m         (:message update)
        chat-id   (-> m :chat :id)
        text      ((or (-> m :text) (-> m :chat :type)))    ; workaround to deal with group type messages
        update-id (:update_id update)
        [command string] (cstr/split text #" " 2)]
    (get-updates token (inc update-id))                     ; marking updates as processed
    [chat-id (or (second (first (filter #(= command (first %)) commands))) ["command not found"])]))

(defn mk-client [bot-token & [chat-id]]
  (let [set-token   (fn [f] (partial f bot-token))
        set-chat-id (fn [f] (if chat-id (partial f chat-id) f))]
    (fn [cmd & args]
      (-> (case cmd
            :set-webhook (-> set-webhook (set-token))
            :get-webhook (-> get-webhook-info (set-token))
            :delete-webhook (-> delete-webhook (set-token))
            :get-updates (-> get-updates (set-token))
            :process-update (-> process-update (set-token))
            :send-message (-> send-message (set-token) (set-chat-id))
            :get-me (-> get-me (set-token))
            :send-photo (-> send-photo-file (set-token) (set-chat-id))
            nil)
          (apply args)))))
