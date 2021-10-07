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
  (let [opts (merge {:chat_id chat-id :text text} opts)]
    (request token "sendMessage" (->snake-case opts))))

(defn send-photo-file [token chat-id photo
                       & [{:keys [caption
                                  parse-mode
                                  caption-entities
                                  disable-notification
                                  reply-to-message-id
                                  allow-sending-without-reply
                                  reply-markup]
                           :as   data}]]
  (let [opts (merge {:chat_id chat-id :photo photo} data)]
    (request token "sendPhoto" (->snake-case opts) {:multipart? true})))

(defn delete-webhook [token & {:keys [drop-pending-updates]
                               :or   {drop-pending-updates false}
                               :as   opts}]
  (request token "deleteWebhook" (->snake-case opts)))

(defn get-webhook-info [token]
  (request token "getWebhookInfo" nil))

(defn set-webhook [token
                   {:keys [url
                           certificate
                           ip-address
                           max-connections
                           allowed-updates
                           drop-pending-updates]
                    :as   data}]
  "For more detailed documentation see
   https://core.telegram.org/bots/api#setwebhook"
  (request token "setWebhook" (->snake-case data)))

(defn get-updates
  ([token] (request token "getUpdates" nil))
  ([token offset] (request token "getUpdates" {:offset offset :limit 1})))

(defn mk-client [bot-token & [chat-id]]
  (let [set-token   (fn [f] (partial f bot-token))
        set-chat-id (fn [f] (if chat-id (partial f chat-id) f))]
    (fn [cmd & args]
      (-> (case cmd
            :set-webhook (-> set-webhook (set-token))
            :get-webhook (-> get-webhook-info (set-token))
            :delete-webhook (-> delete-webhook (set-token))
            :get-updates (-> get-updates (set-token))
            :send-message (-> send-message (set-token) (set-chat-id))
            :get-me (-> get-me (set-token))
            :send-photo (-> send-photo-file (set-token) (set-chat-id))
            nil)
          (apply args)))))
