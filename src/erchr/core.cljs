(ns ^:figwheel-hooks erchr.core
  (:require
    [erchr.macros :refer [read-classes read-stat]]
    [goog.object :as gobj]
    [helix.core :refer [defnc $]]
    [helix.hooks :as hooks]
    [helix.dom :as d]
    ["react-dom" :as rdom]))

(def characters
  (read-classes))

(def ^:dynamic STAT-MIN-VALUE 1)
(def ^:dynamic STAT-MAX-VALUE 99)

(def stat-table
  {:health (read-stat "vigor.csv")
   :focus (read-stat "mind.csv")
   :stamina (read-stat "stamina.csv")
   :equip-load (read-stat "equip-load.csv")})

(defn hex->int [s]
  (js/parseInt s 16))

(defn int->hex [n]
  (.padStart (.toString n 16) 2 "0"))

(defn clamp [n min max]
  (-> n
      (Math/max min)
      (Math/min max)))

(defn rehydrate [state]
  (when (= (count state) 18)
    (let [values (reverse (re-seq #".{1,2}" state))]
      (reduce (fn [memo [stat v]]
                (if (zero? v)
                  memo
                  (assoc memo stat v)))
              {:class (clamp (hex->int (first values)) 0 (count characters))}
              (zipmap
                [:vigor :mind :endurance :strength :dexterity :intelligence :faith :arcane]
                (map #(clamp (hex->int %) 0 STAT-MAX-VALUE)
                     (rest values)))))))

(defn dehydrate [state]
  (reduce (fn [memo k]
            (str (int->hex (get state k 0)) memo))
          ""
          [:class :vigor :mind :endurance :strength :dexterity :intelligence :faith :arcane]))

(defnc attribute
  [{:keys [name value on-change]}]
  (d/div {:class "flex justify-between pv1"}
    (d/label {:htmlFor name} name)
    (d/input {:class "mw4 mw3-ns tr" :name name :type "number" :value value :min STAT-MIN-VALUE :max STAT-MAX-VALUE :pattern "\\d*" :onChange on-change})))

(defnc stat
  [{:keys [name value]}]
  (d/div {:class "flex justify-between pv1"}
    (d/label name)
    (d/label value)))

(defn stat-value
  ([stat]
   (stat-value stat STAT-MIN-VALUE))
  ([stat level]
   (get-in stat-table [stat (dec level) :total])))

(defn char-stat [{:keys [class] :as state} stat]
  (let [character (nth characters class)]
    (get state stat (get character stat))))

(defn app-state []
  (let [params (js/URLSearchParams. js/window.location.search)]
    (if-some [state (.get params "state")]
      (rehydrate state)
      {:class 0})))

(defn rune-level [state]
  (reduce (fn [points stat]
            (+ points (js/parseInt (char-stat state stat))))
          -79
          [:vigor :mind :endurance :strength :dexterity :intelligence :faith :arcane]))

(defn share-url [state]
  (let [a (doto (js/document.createElement "a")
            (gobj/set "href" (str "?state=" (dehydrate state))))]
    (gobj/get a "href")))

(defnc app []
  (let [[state set-state] (hooks/use-state (app-state))
        url (share-url state)]
    (d/div {:class "mv2"}
    ; (d/input {:class "w-100 mv1" :type "text" :placeholder "Tarnished One" :autoFocus true})
      (d/select {:class "w-100 mv1" :defaultValue (:class state) :onChange #(set-state assoc :class (js/parseInt (.. % -target -value)))}
        (for [[idx chr] (map-indexed #(list %1 %2) characters)]
          (d/option {:key (:name chr) :value idx} (:name chr))))
      (d/section {:class "mt3"}
        ($ stat {:name "Level" :value (rune-level state)}))
      (d/section
        (d/h2 {:class "ttu lh-title"} "Attribute Points")
        (map-indexed (fn [idx config]
                       ($ attribute
                          {:key (str idx)
                           :name (:name config)
                           :on-change #(set-state assoc (:attribute config) (js/parseInt (.. % -target -value)))
                           :value (char-stat state (:attribute config))}))
                     [{:name "Vigor" :attribute :vigor}
                      {:name "Mind" :attribute :mind}
                      {:name "Endurance" :attribute :endurance}
                      {:name "Strength" :attribute :strength}
                      {:name "Dexterity" :attribute :dexterity}
                      {:name "Intelligence" :attribute :intelligence}
                      {:name "Faith" :attribute :faith}
                      {:name "Arcane" :attribute :arcane}]))
      (d/section
        (d/h2 {:class "ttu lh-title"} "Base Stats")
        (map-indexed #($ stat {:key (str %1) & %2})
           [{:name "HP" :value (stat-value :health (char-stat state :vigor))}
            {:name "FP" :value (stat-value :focus (char-stat state :mind))}
            {:name "Stamina" :value (stat-value :stamina (char-stat state :endurance))}
            {:name "Equip Load" :value (stat-value :equip-load (char-stat state :endurance))}
            #_ {:name "Discovery" :value 1}]))
      (d/section {:class "mt4"}
        (d/ul {:class "list pl0 flex justify-between"}
          (d/li {:class "dib mr3"}
            (d/a {:class "link pointer dark-blue hover-navy" :target "_blank" :href url} url))
          (d/li {:class "dib"}
            (d/a {:class "link pointer dark-blue hover-navy" :onClick #(set-state select-keys [:class])} "Reset")))))))

(defonce container
  (js/document.getElementById "app"))

(defonce root
  (rdom/createRoot container))

(.render root ($ app) container)

;; specify reload hook with ^:after-load metadata
(defn ^:after-load on-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
