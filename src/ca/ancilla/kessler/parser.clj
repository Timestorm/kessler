(ns ca.ancilla.kessler.parser
  (:import java.lang.Character)
  (:use clearley.core ca.ancilla.kessler.lexer clojure.string))

; an SFS is a sequence of objects and properties, optionally commented
; a comment starts with // and extends to the end of the line
; a property consists of NAME '=' VALUE, where VALUE is an arbitrary string
; running from the first non-whitespace after the '=' to the end of the line
; an object consists of TYPE '{' SFS '}', where TYPE is an allcaps NAME

(def ^:private sfs-lexer
  (lexer
    ["\\s+"           :whitespace :drop-token]
    ["//.*"           :comment    :drop-token]
    ["\\{"            :open-brace]
    ["\\}"            :close-brace]
    ["[a-zA-Z0-9]+"   :key]
    ["=\\s*(.*)"      :value      #(-> %2)]))

(defmacro deftoken [name tag]
  `(def ~name (scanner #(= (:tag %) ~tag) #(:value %))))

(deftoken sfs-key :key)
(deftoken sfs-value :value)
(deftoken open-brace :open-brace)
(deftoken close-brace :close-brace)

(defn- splitvalue
  "Not used yet - may not even want this.
  If v is a scalar value, returns v.
  If v is a vector value, returns a vector of its contents.
  So '1' => '1', and '1,2,3,4' => [1 2 3 4]."
  [v]
  (let [values (split v #",")]
    (if (> (count values) 1)
      values
      v)))

(defn- sfs-conj
  "Conjoin an SFS with an SFS-Item. This means attaching keyvalues to :properties and children to :children."
  [sfs item]
  (if (vector? item)
    (let [[key value] item] (update-in sfs [:properties] assoc key value))
    (update-in sfs [:children] conj item)))

(defrule sfs-item
  ([sfs-key sfs-value] [sfs-key sfs-value])
  ([sfs-key open-brace sfs close-brace] (assoc sfs :type sfs-key)))

(defrule sfs
  ([sfs-item sfs] (sfs-conj sfs sfs-item))
  ([sfs-item] (sfs-conj { :children nil :properties {} } sfs-item)))

(def sfs-parser (build-parser sfs))

(defn sfs-parse [sfs] (assoc (execute sfs-parser (lex-seq sfs-lexer sfs)) :type "SFS"))
