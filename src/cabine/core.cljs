(ns cabine.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! timeout]]
            [cabine.audio :as audio]))

(def noise-node (atom nil))

(defn stop-noise []
  (when @noise-node
    (audio/stop-noise @noise-node)
    (reset! noise-node nil)))

(def cabine-state (atom {:C {:playing false :name :C :img "center_speaker.png"}
                         :L {:playing false :name :L :img "left_speaker.png"}
                         :SL {:playing false :name :SL :img "left_speaker.png"}
                         :R {:playing false :name :R :img "right_speaker.png"}
                         :SR {:playing false :name :SR :img "right_speaker.png"}}))

(defn speaker-playing-view? [playing]
  (when playing " playing"))

(defn speaker-view [speaker owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [play]}]
      (dom/div #js {:className (str "audyx-equipment "
                                 "audyx-equipment-"
                                 (name (:name @speaker))
                                 (speaker-playing-view? (:playing @speaker)))
                    :onClick (fn [e] (if (:playing @speaker) (stop-noise) (put! play @speaker)))}
        (dom/img #js {:src (str "img/" (:img @speaker))
                      :width 52})))))

(defn cabine-5-speakers-view [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [play]}]
      (dom/div nil
        (dom/div #js {:className "row"}
          (om/build speaker-view
            (get app :C)
            {:init-state {:play play}}))
        (dom/div #js {:className "row"}
          (dom/div #js {:className "col-xs-4 col-xs-offset-1"}
            (om/build speaker-view
              (get app :L)
              {:init-state {:play play}}))
          (dom/div #js {:className "col-xs-4 col-xs-offset-2"}
              (om/build speaker-view
                (get app :R)
                {:init-state {:play play}})))
          (dom/div #js {:className "row"}
            (dom/div #js {:className "col-xs-4 col-xs-offset-2"}
              (om/build speaker-view
                (get app :SL)
                {:init-state {:play play}}))
              (dom/div #js {:className "col-xs-4"}
                (om/build speaker-view
                  (get app :SR)
                  {:init-state {:play play}})))))))

(defn cabine-2-speakers-view [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [play]}]
      (dom/div nil
        (dom/div #js {:className "row"} nil)
        (dom/div #js {:className "row"}
          (dom/div #js {:className "col-xs-4 col-xs-offset-1"}
            (om/build speaker-view
              (get app :L)
              {:init-state {:play play}}))
          (dom/div #js {:className "col-xs-4 col-xs-offset-2"}
              (om/build speaker-view
                (get app :R)
                {:init-state {:play play}})))
        (dom/div #js {:className "row"}
          (dom/div #js {:className "col-xs-4 col-xs-offset-2"} nil)
          (dom/div #js {:className "col-xs-4"} nil))))))

(defn cabine-view [app owner]
  (reify

    om/IInitState
    (init-state [_]
      {:play (chan)})

    om/IWillMount
    (will-mount [_]
      (let [play (om/get-state owner :play)]
        (go (loop []
          (let [speaker (<! play)]
            (stop-noise)
            (reset! noise-node (audio/play-noise-in-speaker (:name speaker) cabine-state))
            (recur))))))

    om/IRenderState
    (render-state [this {:keys [play]}]
      (dom/div #js {:className "audyx-container"}
          (dom/div #js {:className "audyx-equipment-wrap"}
            (if (> (audio/num-of-speakers) 2)
              (om/build cabine-5-speakers-view app {:init-state {:play play}})
              (om/build cabine-2-speakers-view app {:init-state {:play play}})))))))

(om/root cabine-view cabine-state {:target (. js/document (getElementById "cabine"))})
