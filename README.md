# clj-telegram

library to work with telegram bot API

## Usage

Method usage
```clojure
(require '[clj-telegram.core :as tg])

(def chat-id 123)
(def token "token")

(tg/send-message token chat-id "Hello there")
```

Client usage
```clojure
(require '[clj-telegram.core :as tg])

(def chat-id 123)
(def token "token")

(def client 
  (tg/mk-client token chat-id))
  
(client :send-message "Hello there")
```

## TODO
* remove http client dependencies
* remove json lib dependencies

## License

Copyright © 2022 yneth

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
