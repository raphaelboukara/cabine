(ns cabine.audio
  (:require [audyx-toolbet.audio :as audio]
            [webaudio.audio-api :as api]
            [webaudio.noise :as noise]
            [audyx-toolbet.collections :as collections]))

(enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;GLOBAL CONTEXT;;;;;;;;;;;;;;;;;;;;;;;
(def global-context (api/create-context))

(defn num-of-speakers []
  (api/num-of-speakers global-context))

;;;;;;;;;;;;;;;;;;;;;AUDIO CONTEXT;;;;;;;;;;;;;;;;;;;;;;;
(defn create-speaker-map [context]
  (let [merger (api/connect-to-destination (.createChannelMerger context))
        node (fn [i]
               (let [volume-node (api/create-gain context)]
                 (api/connect volume-node merger 0 i)
                 {:volume volume-node}))]
    (-> (map-indexed (fn[i x] [x (node i)]) (api/name-of-speakers context))
        (collections/vec->map))))

(defn create-graph []
  (let [context (api/create-context)
        speaker-splitter (api/create-splitter context "explicit")
        volume-node (api/connect (api/create-gain context) speaker-splitter)]
    (api/set-gain volume-node 0)
    {
     :context context
     :speaker-map (create-speaker-map context)
     :speaker-splitter speaker-splitter
     :volume-node volume-node}))

(def graph (create-graph))

(defn switch-speaker-cabine-only [speaker on? cabine]
  (swap! cabine assoc-in [(keyword speaker) :playing] on?))

(defn switch-speaker [speaker on? cabine]
  (api/set-gain (get-in graph [:speaker-map (name speaker) :volume]) (if on? 1 0))
  (switch-speaker-cabine-only speaker on? cabine))

(defn shutup-speakers []
  (doseq [spkr (keys (:speaker-map graph))]
    (api/set-gain (get-in graph [:speaker-map spkr :volume]) 0)))

(defn play-node-in-speaker [source-node speaker volume {:keys [speaker-map speaker-splitter volume-node]} cabine]
  (api/connect source-node volume-node) ;;connect source-node to volume-node
  (shutup-speakers) ;;shut all speakers
  (switch-speaker speaker true cabine) ;;open the speaker
  (api/set-volume-speakers volume-node volume 1) ;;set volume on volume-node
  (api/start source-node) ;;play the node in the speakers list
  (as-> (map :volume (collections/select-vals speaker-map [(name speaker)])) $
        (api/connect-node-to-nodes speaker-splitter $)) ;;connect the splitter to speakers list
  (api/on-ended source-node #(switch-speaker-cabine-only speaker false cabine)) ;;when end playing update cabine status
  source-node)

(defn play-noise-in-speaker [speaker cabine]
  (as-> (get graph :context) $
        (noise/create-white-noise-node $ :num-channels 1)
        (play-node-in-speaker $ speaker -30 graph cabine)))

(defn stop-noise [node]
  (api/stop node))