(ns spam.core
  (:require [clojure.string :as str]
            [incanter.stats :as stats])
  (:use [clojure.java.io :only [file]])
  (:gen-class))

(def min-spam-score 0.9)
(def max-ham-score 0.4)

(defn classify-score [score]
  [(cond
    (<= score max-ham-score) :ham
    (>= score min-spam-score) :spam
    :else :unsure)
   score])

(defrecord Token-Feature [token spam ham])

(defn new-token [token]
  (Token-Feature. token 0 0))

(defn inc-count [token-feature type]
  (update-in token-feature [type] inc))

(def feature-db (agent {} :error-handler #(println "error: " %2)))
(def total-ham (agent 0))
(def total-spam (agent 0))

(defn clear-db []
  (send feature-db (constantly {}))
  (send total-ham (constantly 0))
  (send total-spam (constantly 0)))

(defn update-feature!
  "Looks up a Token-Feature in the database and creates it if it doesn't
  exist, or updates it. f takes the Token-Feature as the first argument
  and args as the rest"
  [token f & args]
  (send feature-db update-in [token]
        #(apply f (if (nil? %1) (new-token token) %1) args)))

;; Can make better choice for tokens...see paulgraham.com/better.html
(defn extract-tokens [text]
  (let [header-fields ["To:"
                       "From:"
                       "Subject:"
                       "Return-Path:"]
        token-regex #"[\w\-\$!]{3,}"]
    (apply
     concat
     (re-seq token-regex text)
     (for [field header-fields]
       (map #(str field %1)  ; appends field to each word from line
            (apply concat    ; gets seq of all words from given header
                   (map (fn [x] (->> x second (re-seq token-regex))) 
                        (re-seq
                         (re-pattern (str field "(.*)\n"))
                         text))))))))

;; In PCL, only uses unique words...don't think that's what we want
(defn update-features!
  "Creates/access token-feature in database for each token in text. Runs
  (apply f token-feature args) for each token-feature"
  [text f & args]
  (doseq [token (extract-tokens text)] ; call set here
    (apply update-feature! token f args)))

(defn extract-features [text]
  (keep (fn [x] x) (map #(@feature-db %1) (extract-tokens text))))

(defn inc-total-count! [type]
  (send (case type
          :spam total-ham
          :ham total-spam)
        inc))

(defn train! [text type]
  (update-features! text inc-count type)
  (inc-total-count! type))

(defn spam-probability [feature]
  (let [s (/ (:spam feature) (max 1 @total-ham))
        h (/ (:ham feature) (max 1 @total-spam))]
      (/ s (+ s h))))

(defn bayesian-spam-probability
  "Calculates probability a feature is spam on a prior. assumed-
  probability is the prior assumed for each feature, and weight is
  weight to be given to the prior (i.e. the number of data points to
  count it as).  Defaults to 1/2 and 1."
  [feature & {:keys [assumed-probability weight] :or
              {assumed-probability 1/2 weight 1}}]
  (let [basic-prob (spam-probability feature)
        total-count (+ (:spam feature) (:ham feature))]
    (/ (+ (* weight assumed-probability)
          (* total-count basic-prob))
       (+ weight total-count))))

(defn fisher
  "Fisher computation described by Robinson."
  [probs num]
  (- 1 (stats/cdf-chisq
        (* -2 (reduce + (map #(Math/log %1) probs)))
        :df (* 2 num))))

(defn score [features]
  (let [spam-probs (map bayesian-spam-probability features)
        ham-probs (map #(- 1 %1) spam-probs)
        num (count features)
        h (- 1 (fisher spam-probs num))
        s (- 1 (fisher ham-probs num))]
     (/ (+ (- 1 h) s) 2)))

(defn classify
  "Returns a vector of the form [classification score]"
  [text]
   (-> text
       extract-features
       score
       classify-score))

(defn train-from-corpus! [corpus]
  (doseq [v corpus]
    (let [[filename type] v]
      (train! (slurp filename) type))))

(defn test-from-corpus [corpus]
  (for [v corpus]
    (let [[filename type] v
          [classification score] (classify (slurp filename))]
      {:filename filename
       :type type
       :classification classification
       :score score})))

(defn test-classifier! [corpus cv-fraction]
  "Tests the classifier on a corups with, leaving cv-fraction for
  cross validation.  Each item in corpus is a vector with the
  following format: [filename type], where type can be :ham or :spam.
  Returns a map with keys :filename, :type, :classification, :score"
    (clear-db)
    (let [shuffled (shuffle corpus)
          size (count corpus)
          training-num (* size (- 1 cv-fraction))
          training-set (take training-num shuffled)]
      (train-from-corpus! training-set)
      (await feature-db)
      (test-from-corpus (nthrest shuffled training-num))))

(defn populate-corpus
  "Returns a list of vectors of the form [filename type]"
  []
  (concat
   (map (fn [file] [(.toString file) :ham])
        (rest (file-seq (file "corpus/ham"))))
   (map (fn [file] [(.toString file) :spam])
        (rest (file-seq (file "corpus/spam"))))))

(defn result-type [{:keys [filename type classification score]}]
  (case type
    :ham (case classification
           :ham :correct
           :spam :false-positive
           :unsure :missed-ham)
    :spam (case classification
            :spam :correct
            :ham :false-negative
            :unsure :missed-spam)))

(defn analyze-results [results]
  (reduce (fn [map result]
            (let [type (result-type result)]
              (update-in map [type] inc)))
          {:total (count results) :correct 0 :false-positive 0
           :false-negative 0 :missed-ham 0 :missed-spam 0}
          results))

(defn print-result [result]
  (let [total (:total result)]
    (doseq [[key num] result]
      (printf "%15s : %-6d%6.2f %%%n"
              (name key) num (float (* 100 (/ num total)))))))

(defn -main [& args]
  (let [frac-string (or (first args) "1/5")
        cv-frac (read-string frac-string)]
    (print-result
     (analyze-results (test-classifier! (populate-corpus) cv-frac)))))