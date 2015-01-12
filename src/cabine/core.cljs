(ns cabine.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! timeout]]
            [cabine.audio :as audio]))

(def noise-node (atom nil))

(def cabine-state (atom {:title "Cabine"
                         :C {:playing false :name :C}
                         :L {:playing false :name :L}
                         :SL {:playing false :name :SL}
                         :R {:playing false :name :R}
                         :SR {:playing false :name :SR}}))

(defn speaker-playing-view? [playing]
  (when playing " playing"))

(defn speaker-view [speaker owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [play]}]
      (dom/div #js {:className (str "audyx-equipment"
                                 (speaker-playing-view? (:playing speaker)))
                    :onClick (fn [e] (put! play @speaker))}
        (dom/img #js {:src "img/speaker.png"
                      :width 55})))))

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
          (dom/div #js {:className "col-md-4 col-md-offset-1"}
            (om/build speaker-view
              (get app :L)
              {:init-state {:play play}}))
          (dom/div #js {:className "col-md-4 col-md-offset-2"}
              (om/build speaker-view
                (get app :R)
                {:init-state {:play play}})))
          (dom/div #js {:className "row"}
            (dom/div #js {:className "col-md-4 col-md-offset-2"}
              (om/build speaker-view
                (get app :SL)
                {:init-state {:play play}}))
              (dom/div #js {:className "col-md-4"}
                (om/build speaker-view
                  (get app :SR)
                  {:init-state {:play play}})))))))

(defn cabine-2-speakers-view [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [play]}]
      (dom/div nil
        (dom/div #js {:className "row"}
          (dom/div #js {:className "col-md-4 col-md-offset-1"}
            (om/build speaker-view
              (get app :L)
              {:init-state {:play play}}))
          (dom/div #js {:className "col-md-4 col-md-offset-2"}
              (om/build speaker-view
                (get app :R)
                {:init-state {:play play}})))))))

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
            (when @noise-node
              (audio/stop-noise @noise-node)
              (reset! noise-node nil))
            (reset! noise-node (audio/play-noise-in-speaker (:name speaker) cabine-state))
            (recur))))))

    om/IRenderState
    (render-state [this {:keys [play]}]
      (dom/div nil
        (dom/h1 nil (:title app))
        (dom/div #js {:className "audyx-equipment-wrap"}
          (if (> (audio/num-of-speakers) 2)
            (om/build cabine-5-speakers-view app {:init-state {:play play}})
            (om/build cabine-2-speakers-view app {:init-state {:play play}})))))))

(om/root cabine-view cabine-state {:target (. js/document (getElementById "cabine"))})
