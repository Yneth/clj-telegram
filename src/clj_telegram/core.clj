(ns clj-telegram.core
  (:require [clojure.string :as cstr]
            [clojure.data.json :as cjson]))

(def api-url "https://api.telegram.org/bot")

(def http-post-f
  (delay
    (ns-resolve 'org.httpkit.client (symbol "post"))))
(defn http-post [& args]
  @(apply @http-post-f args))

(defn- request [token action data]
  (let [url      (str api-url token "/" action)
        response (http-post url {:form-params data
                                 :as          :text})
        json     (cjson/read-str (:body response) :key-fn keyword)]
    json))

(defn get-me [token]
  (request token "getMe" nil))

(defn send-message [token chat-id text]
  (request
    token
    "sendMessage"
    {:chat_id chat-id
     :text    text})
  true)

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
