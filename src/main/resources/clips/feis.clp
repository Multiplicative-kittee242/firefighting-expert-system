;--------------------------------------------------------------------------------------------------------
; Определение шаблонов и исходного состояния
;--------------------------------------------------------------------------------------------------------

(defclass MAIN::TANK
	(is-a	USER))

(defclass MAIN::DECK
	(is-a	TANK)
	(slot	tank	(default	3)))

(defclass MAIN::LOCATION
	(is-a	DECK)
	(slot	title	(type	LEXEME))
	(slot	tank	(default	3))
	(slot	type	(type	LEXEME)
			(default	none))
	(slot	accedent	(type	LEXEME)
			(default	none))
	(slot	area)
	(slot	evacuation (type	LEXEME)
			(default	none))
	(slot	ventil	(default	none))
	(slot	explosive	(default	none))
	(slot	burning	(default	none))
	(slot	machinery (default	none))
	(slot	co	(default	none)))

(defclass MAIN::DOOR
	(is-a	DECK)
	(slot	from	(type	LEXEME))
	(slot	to	(type	LEXEME))
	(slot	status	(type	LEXEME)
			(default	open)))

(defclass MAIN::HYDRANT
	(is-a	DECK)
	(slot	title	(type	LEXEME))
	(slot	location	(type	LEXEME))
	(slot	number)
	(slot	free))

(defclass MAIN::EXTINGUISHER
	(is-a	DECK)
	(slot	title	(type	LEXEME))
	(slot	location	(type	LEXEME))
	(slot	type	(type	LEXEME))
	(slot	used	(type	LEXEME)
			(default	no)))

(defclass MAIN::BORDER
	(is-a	DECK)
	(slot	from	(type	LEXEME))
	(slot	upon	(type	LEXEME))
	(slot	length)
	(slot	fire-line	(default	none)))

(defclass MAIN::EVACUATION
	(is-a	USER)
	(slot	from	(type	LEXEME))
	(slot	to	(type	LEXEME)))

(defclass MAIN::FIRE-DISTANCE
	(is-a	USER)
	(multislot	from)
	(multislot	to)
	(slot	value))

(defclass MAIN::BORDER-DISTANCE
	(is-a	USER)
	(multislot	from)
	(multislot	to)
	(slot	value))

(defclass MAIN::ACTION
	(is-a	USER)
	(slot	phase	(type	LEXEME))
	(slot	location	(type	LEXEME))
	(slot	object	(type	LEXEME))
	(slot	to-do	(type	LEXEME)))

(defclass MAIN::EXPLAIN
	(is-a	USER)
	(slot	title)
	(slot	location	(default	none))
	(slot	from	(default	none))
	(slot	to	(default	none))
	(slot	type	(default	none))
	(slot	antec)
	(multislot	antec1	(default	none))
	(multislot	antec2	(default	none))
	(multislot	consec))

;---------------------------------------------------------------------------------------------------------
; LOCATION, HYDRANT, BORDER, DOOR, EVACUATION, FIRE-DISTANCE and EXTINGUISHER instances are seeded from
; the app's own topology config (see domain.registry.TopologyModel + config/topology.yaml), via
; ClipsEngineAccess.initializeTopology(), which creates them directly after reset(). DOOR includes the
; exits to another deck (door_*_to_out); BORDER is created in both directions; EVACUATION is the directed
; escape graph (evac_*_to_*, each direction a separate instance); FIRE-DISTANCE is the door-to-door and
; hydrant-to-door hose-reach graph (hosespan_*), used by the hydrant search to judge whether a hose can
; reach a given door; EXTINGUISHER is the set of portable devices (identity + type), instance-named by
; their own title. LOCATION is created fully in one step, including every scenario attribute (area,
; tank, compartment type, ventilation, explosive/burning material, machinery, chemical-suppression
; system) — the app now owns all of it (domain.Location), so no location-attrs facts remain here.
;---------------------------------------------------------------------------------------------------------

(deftemplate to-check
	(slot	what)
	(slot	root))

(deftemplate been-check		; Указывает, какой путь уже был проделан
	(slot	to)		; В какое помещение
	(slot	from))		; Из какого помещения

(deftemplate evacuate-locations	; По мере проверки дерева помещений накапливает имена пройденных
	(multislot	locations))

(deftemplate dead			; Помечает тупиковые эвакуируемые помещения
	(slot	location))

(deftemplate out			; Помечает помещения, имеющие выход в другой отсек
	(slot	location))

(deftemplate hydrant-potential
	(slot	title)
	(slot	distance)
	(slot	location)
	(slot	path)
	(slot	root)
	(slot	weight-p	(default	0))
	(slot	weight-n	(default	0)))

(deftemplate hydrant-reserve
	(slot	title)
	(slot	distance)
	(slot	location)
	(slot	path)
	(slot	root)
	(slot	weight-p	(default	0))
	(slot	weight-n	(default	0))
	(slot	for-ext))

(deftemplate hydrant-shadow
	(slot	title)
	(slot	distance)
	(slot	location)
	(slot	path)
	(slot	root)
	(slot	weight-p	(default	0))
	(slot	weight-n	(default	0))
	(slot	for-ext)
	(slot	created	(default	manual)))

(deftemplate hydrant-positive
	(slot	title)
	(slot	root)
	(slot	negative))

(deftemplate hydrant-in-use
	(slot	title)
	(slot	distance)
	(slot	location)
	(slot	path)
	(slot	root)
	(slot	weight-n)
	(slot	for-ext)
	(slot	created	(default	manual))
	(slot	id))

(deftemplate check-for-hydrant
	(slot	what)
	(slot	root)
	(slot	past)
	(slot	path)
	(slot	distance)
	(slot	number)
	(slot	checked	(default	"")))

(deftemplate move-hydrant
	(slot	root)
	(slot	past)
	(slot	present)
	(slot	path)
	(slot	distance))

(deftemplate fire-line-location
	(slot	location)
	(slot	target)
	(slot	perimeter)
	(slot	hydrants-need)
	(slot	hydrants-here	(default	0))
	(multislot	hydrants-titles))

(deftemplate hydrants-search-progress
	(slot	location))

(deftemplate select-max-priority
	(slot	location)
	(slot	priority))

(deftemplate select-min-alternatives
	(slot	location)
	(slot	alternatives))

(deftemplate select-min-doors
	(slot	location)
	(slot	hydrant)
	(slot	path-doors))

(deftemplate select-min-length
	(slot	location)
	(slot	hydrant)
	(slot	length))

(deftemplate allocate
	(slot	location)
	(slot	hydrant))

(deftemplate ext-graph
	(slot	from)
	(slot	to)
	(multislot	hydrants-b-titles))

(deftemplate ext-edge
	(slot	location)
	(slot	to-needs)
	(slot	by-border	(default	0))
	(slot	status		(default	wait))
	(multislot	hydrants-titles))

(deftemplate ext-array
	(multislot	letters)
	(slot	index)
	(multislot	branch)
	(slot	price		(default	0))
	(slot	checked		(default	no)))

(deftemplate no-fire
	(slot	location))

(deftemplate plan
	(slot	number		(default	1))
	(slot	location)
	(multislot	from))

(deftemplate plan-number
	(multislot	locations)
	(slot	last-number))


;---------------------------------------------------------------------------------------------------------
; Эвакуация помещений							3 функции
;---------------------------------------------------------------------------------------------------------

(deffunction MAIN::collect-evac-accedent (?slot-value)					;= При заданном событии (слот ACCEDENT) собирает
	(bind	?collection "")							;    список всех помещений, имеющих такое событие
	(do-for-all-instances ((?l LOCATION))
		(eq ?l:accedent ?slot-value) (bind ?collection (str-cat ?collection ?l:title)))
	(return	?collection))

(deffunction MAIN::collect-evac-evacuation (?slot-value)					;= При заданном состоянии (слот EVACUATION) собирает
	(bind	?collection "")							;    список всех помещений, имеющих такое состояние
	(do-for-all-instances ((?l LOCATION))
		(eq ?l:evacuation ?slot-value) (bind ?collection (str-cat ?collection ?l:title)))
	(return	?collection))

(deffunction MAIN::get-explanation-evac (?slot ?slot-value)
	(bind	?collection "")
	(do-for-instance ((?e EXPLAIN))
		(and	(eq ?e:title evacuation)
			(eq ?e:location ?slot-value))
		(bind ?collection ?e:?slot))
	(return	?collection))


;---------------------------------------------------------------------------------------------------------
; Герметизация помещений							4 функции
;---------------------------------------------------------------------------------------------------------

(deffunction MAIN::arrange-letters (?letter1 ?letter2)
	(if	(< 0 (str-compare ?letter1 ?letter2))
		then	(return	(explode$ (str-cat	?letter2 " " ?letter1)))
		else	(return	(explode$ (str-cat	?letter1 " " ?letter2)))))

(deffunction MAIN::collect-germ-loc (?slot-value)						;= При заданном событии (значении слота) собирает
	(bind	?collection "")							;    список всех помещений, имеющих такое событие
	(do-for-all-instances ((?l LOCATION))
		(eq ?l:ventil ?slot-value)
		(bind ?collection (str-cat ?collection ?l:title)))
	(return	?collection))

(deffunction MAIN::collect-germ-door (?slot-value)					;= При заданном событии (значении слота) собирает
	(bind	?collection "")							;    список всех помещений, имеющих такое событие
	(do-for-all-instances ((?d DOOR))
		(eq ?d:status ?slot-value)
		(bind	?temp (arrange-letters ?d:to ?d:from))
		(bind	?collection (str-cat ?collection (nth$ 1 ?temp) (nth$ 2 ?temp))))
	(return	?collection))

(deffunction MAIN::get-explanation (?slot ?slot-value)
	(bind	?collection "")
	(do-for-instance ((?e EXPLAIN))
		(and	(eq ?e:title germetisation)
			(eq ?e:location ?slot-value))
		(bind ?collection ?e:?slot))
	(return	?collection))

(deffunction MAIN::get-explanation2 (?slot ?slot1-value ?slot2-value)
	(bind	?collection "")
	(do-for-instance ((?e EXPLAIN))
		(and	(eq ?e:title germetisation)
			(eq ?e:from ?slot1-value)
			(eq ?e:to ?slot2-value))
		(bind ?collection ?e:?slot))
	(return	?collection))


;---------------------------------------------------------------------------------------------------------
; Предотвращение взрывов							3 функции
;---------------------------------------------------------------------------------------------------------

(deffunction MAIN::get-explanation-expl (?slot ?slot-value)
	(bind	?collection "")
	(do-for-instance ((?e EXPLAIN))
		(and	(eq ?e:title explosion)
			(eq ?e:location ?slot-value))
		(bind ?collection ?e:?slot))
	(return	?collection))

(deffunction MAIN::collect-action-phase (?phase)
	(bind	?collection "")
	(do-for-all-instances ((?a ACTION))
		(and	(eq ?a:phase ?phase)
			(neq ?a:to-do done))
		(bind ?collection (str-cat ?collection ?a:location)))
	(return	?collection))

(deffunction MAIN::action-edit (?phase ?location ?value)
	(do-for-all-instances ((?a ACTION))
		(and	(eq ?a:phase ?phase)
			(eq ?a:location ?location))
		(and	(send ?a put-to-do ?value)
			(printout t "Сделано" crlf))))


;---------------------------------------------------------------------------------------------------------
; Изоляция горючих материалов						4 функции
;---------------------------------------------------------------------------------------------------------

(deffunction MAIN::collect-isol-mech (?slot-value)						;= При заданном событии (значении слота) собирает
	(bind	?collection "")							;    список всех помещений, имеющих такое событие
	(do-for-all-instances ((?l LOCATION))
		(eq ?l:machinery ?slot-value)
		(bind ?collection (str-cat ?collection ?l:title)))
	(return	?collection))

(deffunction MAIN::get-explanation-isol (?slot ?slot-value)
	(bind	?collection "")
	(do-for-instance ((?e EXPLAIN))
		(and	(eq	?e:title isolation)
			(eq	?e:location ?slot-value))
		(bind	?collection ?e:?slot))
	(return	?collection))

(deffunction MAIN::get-explanation-isol-mech (?slot ?slot-value)
	(bind	?collection "")
	(do-for-instance ((?e EXPLAIN))
		(and	(eq	?e:title isolation)
			(eq	?e:type mech)
			(eq	?e:location ?slot-value))
		(bind	?collection ?e:?slot))
	(return	?collection))

(deffunction MAIN::action-edit-isol-mech (?phase ?location ?value)
	(do-for-all-instances ((?a ACTION))
		(and	(eq ?a:phase ?phase)
			(eq ?a:location ?location)
			(eq ?a:type mech))
		(and	(send ?a put-to-do ?value)
			(printout t "Сделано" crlf))))


;--------------------------------------------------------------------------------------------------------
; Подтягивание сил								5 функций
;--------------------------------------------------------------------------------------------------------

(deffunction MAIN::count-hydrants (?location)
	(bind	?number 0)
	(do-for-all-instances ((?h HYDRANT))
		(eq	?h:location ?location)
		(bind	?number (+ ?number 1)))
	(return	?number))

(deffunction MAIN::count-hydrant-outs (?location)
	(bind	?number 0)
	(do-for-all-instances ((?h HYDRANT))
		(eq	?h:location ?location)
		(bind	?number	(+ ?number ?h:free)))
	(return	?number))

(deffunction MAIN::get-fire ()
	(bind	?fire "")
	(do-for-all-instances ((?l LOCATION))
		(eq	?l:accedent fire)
		(bind	?fire (sym-cat ?fire ?l:title)))
	(return	?fire))

(deffunction MAIN::get-emergent ()
	(bind	?emergent "")
	(do-for-all-instances ((?l LOCATION))
		(eq	?l:accedent fire)
		(bind	?emergent (sym-cat ?emergent ?l:title)))
	(do-for-all-instances ((?l LOCATION))
		(or	(eq	?l:evacuation to-evacuate)
			(eq	?l:evacuation done))
		(bind	?emergent (sym-cat ?emergent ?l:title)))
	(return	?emergent))

(deffunction MAIN::count-potentials (?location)
	(bind	?number 0)
	(do-for-all-facts ((?p hydrant-potential))
		(eq	?p:root ?location)
		(bind	?number (+ ?number 1)))
	(return	?number))

(deffunction MAIN::count-reserves (?location)
	(bind	?number 0)
	(do-for-all-facts ((?hr hydrant-reserve))
		(eq	?hr:root ?location)
		(bind	?number (+ ?number 1)))
	(return	?number))

(deffunction MAIN::count-weight-positive (?location)
	(bind	?perimeter 0.0)
	(do-for-fact ((?fl fire-line-location))
		(eq	?fl:location ?location)
		(bind	?perimeter ?fl:perimeter))
	(bind	?door-to-fire	(length$	(find-instance ((?d DOOR))
						(or	(and	(eq	?d:from ?location)
								(str-index ?d:to (get-emergent)))
							(and	(eq	?d:to ?location)
								(str-index ?d:from (get-emergent)))))))
	(bind	?alternatives	(count-reserves	?location))
	(bind	?weigth	(+	(*	2	?perimeter)
				(*	10	?door-to-fire)
				(*	-1	?alternatives)))
;	(printout	t "Вычислен вес. Петриметр: " ?perimeter ". Дверь: " ?door-to-fire ". Альтернатив: " ?alternatives "."  crlf)
	(return	?weigth))

(deffunction MAIN::count-weight-negative (?doors ?length)
	(bind	?weigth	(+	(*	10	?doors)
				(*	1	?length)))
	(return	?weigth))

(deffunction MAIN::get-line1-borders ()
	(bind	?collection "")
	(do-for-all-instances ((?b BORDER))
		(eq ?b:fire-line line1)
		(bind	?temp (arrange-letters ?b:from ?b:upon))
		(bind	?collection (str-cat ?collection (nth$ 1 ?temp) (nth$ 2 ?temp))))
	(return	?collection))

(deffunction MAIN::get-fire-line-locations ()
	(bind	?collection "")
	(do-for-all-facts ((?fl fire-line-location))
		TRUE
		(bind	?collection (str-cat ?collection ?fl:location)))
	(return	?collection))

(deffunction MAIN::get-fire-line-hydr (?location ?slot)
	(bind	?collection "")
	(do-for-fact ((?fl fire-line-location))
		(eq	?fl:location ?location)
		(bind	?collection (str-cat ?collection ?fl:?slot)))
	(return	?collection))

(deffunction MAIN::get-hydrant-free-outs (?title)
	(bind	?number 0)
	(do-for-all-instances ((?h HYDRANT))
		(eq	?h:title ?title)
		(bind	?number	?h:free))
	(return	?number))

(deffunction MAIN::get-hydrant-total-outs (?title)
  (bind ?number 0)
  (do-for-all-instances ((?h HYDRANT))
    (eq ?h:title ?title)
    (bind ?number ?h:number))
  (return ?number))

(deffunction MAIN::get-hydr-for-location (?location)
	(bind	$?collection "")
	(do-for-fact ((?fl fire-line-location))
		(eq	?fl:location ?location)
		(bind	$?collection ?fl:hydrants-titles))
	(return	$?collection))

(deffunction MAIN::get-extinguishers-for-location (?location)					;= Собирает через пробел титулы неиспользованных
	(bind	?collection "")							;    огнетушителей в указанном помещении
	(do-for-all-instances ((?e EXTINGUISHER))
		(and	(eq	?e:location ?location)
			(eq	?e:used no))
		(bind	?collection (str-cat ?collection ?e:title " ")))
	(return	?collection))

(deffunction MAIN::get-hydrant-length (?location ?hydrant)
	(bind	?collection "")
	(do-for-fact ((?hu hydrant-in-use))
		(and	(eq	?hu:root ?location)
			(eq	?hu:title ?hydrant))
		(bind	?collection ?hu:distance))
	(return	?collection))


;--------------------------------------------------------------------------------------------------------
; Тушение пожара							5 функций
;--------------------------------------------------------------------------------------------------------

(deffunction MAIN::count-to-need (?area)
	(return	(round	(+	(/	?area 15) 0.5))))

(deffunction MAIN::get-ext-hydr (?location)
	(bind	?sum 0)
	(do-for-fact ((?ee ext-edge))
		(eq	?ee:location ?location)
		(bind	?sum (+ ?sum (length$ ?ee:hydrants-titles))))
	(do-for-all-facts ((?eg ext-graph))
		(eq	?eg:to ?location)
		(bind	?sum (+ ?sum (length$ ?eg:hydrants-b-titles))))
	(return	?sum))

(deffunction MAIN::get-count-to-need (?location)
	(bind	?area 0.0)
	(do-for-instance ((?l LOCATION))
		(eq	?l:title ?location)
		(bind	?area	?l:area))
	(return	(round	(+	(/	?area 15.0) 0.5))))

(deffunction MAIN::get-ext-for-location (?location)
	(bind	$?collection (explode$ ""))
	(do-for-fact ((?ee ext-edge))
		(eq	?ee:location ?location)
		(bind	$?collection (create$ $?collection ?ee:hydrants-titles)))
	(return	$?collection))

(deffunction MAIN::get-ext-b-to-for-location (?location)
	(bind	$?collection (explode$ ""))
	(do-for-all-facts ((?eg ext-graph))
		(eq	?eg:to ?location)
		(bind	$?collection (create$ $?collection ?eg:hydrants-b-titles)))
	(return	$?collection))

(deffunction MAIN::get-ext-b-from-for-location (?location)
	(bind	$?collection (explode$ ""))
	(do-for-all-facts ((?eg ext-graph))
		(eq	?eg:from ?location)
		(bind	$?collection (create$ $?collection ?eg:hydrants-b-titles)))
	(return	$?collection))

(deffunction MAIN::get-graph-from-locations ()
	(bind	?collection "")
	(do-for-all-facts ((?eg ext-graph))
		TRUE
		(bind	?collection (str-cat ?collection ?eg:from)))
	(return	?collection))

(deffunction MAIN::get-plan-from (?location)
	(do-for-fact ((?p plan))
		(eq	?location ?p:location)
		(return	?p:from)))

(deffunction MAIN::get-plan-number (?location)
	(do-for-fact ((?p plan))
		(eq	?location ?p:location)
		(return	?p:number)))






;--------------------------------------------------------------------------------------------------------
; Главный модуль
;--------------------------------------------------------------------------------------------------------

(defmodule MAIN
	(export	?ALL))


;(defrule	MAIN::get-fire									;= Получение аварийного помещения от оператора
;	=>
;	(printout	t "Укажите координаты аварийных помещений: ")
;	(bind	?fire-location (read))							;= Считывает координату помещения с клавиатуры
;	(do-for-instance ((?l LOCATION))							;= Добавляет атрибут пожара к свойствам помещения
;		(eq ?l:title ?fire-location)
;		(send ?l put-accedent fire))
;	(printout	t crlf "ТРЕВОГА! Пожар в помещении: " ?fire-location "." crlf))


(defrule	MAIN::initiate
	(declare	(salience	10))
	(object	(is-a	LOCATION)
		(accedent	fire))
	=>
	(focus	EXTINGUISHING)
	(focus	LOCALISATION)
	(focus	IMMEDIATE))


;--------------------------------------------------------------------------------------------------------
; Модуль проведения первоочередных мероприятий
;--------------------------------------------------------------------------------------------------------


(defmodule IMMEDIATE
	(export	?ALL)
	(import	MAIN ?ALL))


(defrule	IMMEDIATE::initiate
	(object	(is-a	LOCATION)
		(accedent	fire))
	=>
	(printout	t crlf "1. ПЕРВООЧЕРЕДНЫЕ МЕРОПРИЯТИЯ:" crlf)
	(focus	IMMEDIATE-ISOLATION)
	(focus	IMMEDIATE-EXPLOSION)
	(focus	IMMEDIATE-GERMETISATION)
	(focus	IMMEDIATE-EVACUATION)
	(focus	IMMEDIATE-EXTINGUISHERS)
	(focus	IMMEDIATE-THREATENED))


;--------------------------------------------------------------------------------------------------------
; Определение помещений угрожаемой зоны
;--------------------------------------------------------------------------------------------------------

(defmodule IMMEDIATE-THREATENED
	(export	?ALL)
	(import	MAIN ?ALL))


(defrule	IMMEDIATE-THREATENED::search						;= Определяет помещения угрожаемой зоны
	(object	(is-a	LOCATION)							;= Получает адрес (каждого) аварийного помещения
		(accedent	fire)
		(title	?fire-location))
	(object	(is-a	BORDER)								;= Выбирает (каждое) смежное помещение
		(from	?fire-location)							;    для обработки в качестве угрождаемого
		(upon	?threatened-location))
	=>
	(do-for-instance ((?l LOCATION))						;= Добавляет атрибут угрозы к свойствам
		(and	(eq	?l:title ?threatened-location)					;    каждого соседнего помещения за исключением
			(not	(eq	?l:accedent fire)))					;    имеющих атрибут пожара
		(send	?l put-accedent threat))
	(printout	t "   Угрожаемая зона. Помещение " (upcase ?threatened-location) "." crlf))


;---------------------------------------------------------------------------------------------------------
; Применение огнетушителей
;---------------------------------------------------------------------------------------------------------

(defmodule IMMEDIATE-EXTINGUISHERS
	(export	?ALL)
	(import	MAIN ?ALL))


(defrule	IMMEDIATE-EXTINGUISHERS::use-local
	(object	(is-a	LOCATION)
		(accedent	fire)
		(title	?fire-location))
;	(or	(object	(is-a	DOOR)
;			(from	?fire-location)
;			(to	?next-location))
;		(object	(is-a	DOOR)
;			(from	?fire-location)
;			(to	?next-location)))
	(object	(is-a	EXTINGUISHER)
		(location	?fire-location)
		(used	no)
		(title	?est-title)
		(type	?est-type))
	=>
	(bind	?location (sub-string 5 5 ?est-title))
	(if	(eq	?est-type co)
		then	(printout	t "   Ликвидация очага возгорания. Применить углекислотный огнетушитель \""
				?est-title "\", находящийся в помещении " (upcase ?location) "." crlf)
		else	(printout	t "   Ликвидация очага возгорания. Если позволяет обстановка, применить пенный огнетушитель \""
				?est-title "\", находящийся в помещении " (upcase ?location) "." crlf)))


;---------------------------------------------------------------------------------------------------------
; Эвакуация помещений
;---------------------------------------------------------------------------------------------------------

(defmodule IMMEDIATE-EVACUATION
	(export	?ALL)
	(import	MAIN ?ALL))


(defrule	IMMEDIATE-EVACUATION::search-initial
	(object	(is-a	LOCATION)
		(accedent	fire)
		(title	?fire-location))
	(object	(is-a	EVACUATION)
		(to	?fire-location)
		(from	?check-location))
	(not	(object	(is-a	LOCATION)
			(title	?check-location)
			(evacuation done)))
	(not	(object	(is-a	EVACUATION)
			(from	?fire-location)
			(to	?check-location)))
	=>
	(do-for-instance ((?l LOCATION))
		(eq	?l:title ?check-location)
		(send	?l put-evacuation to-evacuate))
	(assert	(to-check	(what	?check-location)
			(root	?fire-location)))
	(make-instance of EXPLAIN
		(title	evacuation)
		(location	?check-location)
		(antec	2)
		(antec1	Evacuation path is cut by fire in ?fire-location)
		(antec2	Location ?check-location haven't been evacuated)
		(consec	Evacuate location ?check-location))
	(printout t "   Эвакуация. Помещение " (upcase ?check-location) "." crlf))


(defrule	IMMEDIATE-EVACUATION::search-move
	?check <-	(to-check	(what	?present-location)
			(root	?root))
	(object	(is-a	EVACUATION)
		(from	?check-location)
		(to	?present-location))
	(not	(object	(is-a	EVACUATION)
			(from	?present-location)
			(to	?check-location)))
	(not	(object	(is-a	LOCATION)
			(title	?check-location)
			(evacuation done)))
	=>
	(do-for-instance ((?l LOCATION))
		(eq	?l:title ?check-location)
		(send	?l put-evacuation to-evacuate))
;	(retract	?check)
	(assert	(to-check	(what	?check-location)
			(root	?root)))
	(make-instance of EXPLAIN
		(title	evacuation)
		(location	?check-location)
		(antec	2)
		(antec1	Evacuation path is cut by fire in ?root)
		(antec2	Location ?check-location haven't been evacuated)
		(consec	Evacuate location ?check-location))
	(printout t "   Эвакуация. Помещение " (upcase ?check-location) "." crlf))


(defrule	IMMEDIATE-EVACUATION::burn-garbage
	(declare	(salience	-10))
	?check <-	(to-check)
	=>
	(retract	?check))


;---------------------------------------------------------------------------------------------------------
; Герметизация помещений						2 правила, 4 функции
;---------------------------------------------------------------------------------------------------------

(defmodule IMMEDIATE-GERMETISATION
	(export	?ALL)
	(import	MAIN ?ALL))


(defrule	IMMEDIATE-GERMETISATION::turn-off-aggregates
	(or	(object	(is-a	LOCATION)
			(accedent	fire|threat)
			(ventil	on)
			(title	?location))
		(object	(is-a	LOCATION)
			(evacuation to-evacuate)
			(ventil	on)
			(title	?location)))
	=>
	(bind	?why "")
	(do-for-instance ((?l LOCATION))
		(eq	?l:title ?location)
		(and	(send	?l put-ventil to-off)
			(bind	?why ?l:accedent)))
	(if	(eq ?why fire)
		then	(make-instance of EXPLAIN
				(title	germetisation)
				(location	?location)
				(antec	2)
				(antec1	Location ?location is in fire)
				(antec2	Ventilation in ?location is on)
				(consec	Turn-off ventilation aggregate in ?location)))
	(if	(eq ?why threat)
		then	(make-instance of EXPLAIN
				(title	germetisation)
				(location	?location)
				(antec	2)
				(antec1	Location ?location is in threat)
				(antec2	Ventilation in ?location is on)
				(consec	Turn-off ventilation aggregate in ?location)))
	(if	(and	(neq ?why fire)
			(neq ?why threat))
		then	(make-instance of EXPLAIN
				(title	germetisation)
				(location	?location)
				(antec	2)
				(antec1	Location ?location is to-evacuate)
				(antec2	Ventilation in ?location is on)
				(consec	Turn-off ventilation aggregate in ?location)))
	(printout	t "   Герметизация. Отключить вентиляцию: " (upcase ?location) "." crlf)
)


(defrule	IMMEDIATE-GERMETISATION::close-doors
	(or	(object	(is-a	LOCATION)
			(accedent	fire|threat)
			(title	?location))
		(object	(is-a	LOCATION)
			(evacuation to-evacuate|done)
			(title	?location)))
	(or	(object	(is-a	DOOR)
			(from	?location)
			(status	open)
			(to	?to&~out))
		(object	(is-a	DOOR)
			(to	?location)
			(status	open)
			(from	?to&~out)))
	=>
	(bind	?temp (arrange-letters ?location ?to))
	(do-for-instance ((?d DOOR))
		(and	(eq	?d:from (nth$ 1 ?temp))
			(eq	?d:to (nth$ 2 ?temp)))
		(send	?d put-status to-close))
	(make-instance of EXPLAIN
		(title	germetisation)
		(from	?location)
		(to	?to)
		(antec	2)
		(antec1	Locations ?location and ?to are in emergency)
		(antec2	Door is open)
		(consec	Close door from ?location to ?to))
	(make-instance of EXPLAIN
		(title	germetisation)
		(from	?to)
		(to	?location)
		(antec	2)
		(antec1	Locations ?to and ?location are in emergency)
		(antec2	Door is open)
		(consec	Close door from ?to to ?location))
	(printout	t "   Герметизация. Задраить дверь из " (upcase ?to) " в " (upcase ?location) "." crlf)
)


;---------------------------------------------------------------------------------------------------------
; Предотвращение взрывов						3 правила, 3 функции
;---------------------------------------------------------------------------------------------------------

(defmodule IMMEDIATE-EXPLOSION
	(export	?ALL)
	(import	MAIN ?ALL))

(defrule	IMMEDIATE-EXPLOSION::diesel-oil
	(or	(object	(is-a	LOCATION)
			(accedent	fire|threat)
			(explosive diesel_oil)
			(title	?location))
		(object	(is-a	LOCATION)
			(evacuation to-evacuate|done)
			(explosive diesel_oil)
			(title	?location)))
	(not	(object	(is-a	ACTION)
			(phase	explosion)
			(location	?location)
			(object	diesel_oil)
			(to-do	pump_out|done)))
	=>
	(make-instance of ACTION
		(phase	explosion)
		(location	?location)
		(object	diesel_oil)
		(to-do	pump_out))
	(make-instance of EXPLAIN
		(title	explosion)
		(location	?location)
		(antec	2)
		(antec1	Location ?location is in emergency)
		(antec2	Possibility of diesel oil explosion)
		(consec	Pump out diesel oil in ?location))
	(printout	t "   Предотвращение взрывов. Откачать дизельное топливо из помещения " (upcase ?location) " во избежание взрыва." crlf))


(defrule	IMMEDIATE-EXPLOSION::compressed-air
	(or	(object	(is-a	LOCATION)
			(accedent	fire|threat)
			(explosive compressed_air)
			(title	?location))
		(object	(is-a	LOCATION)
			(evacuation to-evacuate|done)
			(explosive compressed_air)
			(title	?location)))
	(not	(object	(is-a	ACTION)
			(phase	explosion)
			(location	?location)
			(object	compressed_air)
			(to-do	carry_out|done)))
	=>
	(make-instance of ACTION
		(phase	explosion)
		(location	?location)
		(object	compressed_air)
		(to-do	carry_out))
	(make-instance of EXPLAIN
		(title	explosion)
		(location	?location)
		(antec	2)
		(antec1	Location ?location is in emergency)
		(antec2	Possibility of compressed air cylinders explosion)
		(consec	Set air on to fight or carry out cylinders from ?location))
	(printout	t "   Предотвращение взрывов. Стравить или вынести воздушные баллоны из помещения " (upcase ?location) " во избежание взрыва." crlf))


(defrule	IMMEDIATE-EXPLOSION::chemical-reagent
	(or	(object	(is-a	LOCATION)
			(accedent	fire)
			(explosive chemical_reagent)
			(title	?location))
		(object	(is-a	LOCATION)
			(evacuation to-evacuate|done)
			(explosive chemical_reagent)
			(title	?location)))
	(not	(object	(is-a	ACTION)
			(phase	explosion)
			(location	?location)
			(object	chemical_reagent)
			(to-do	to_fight|done)))
	=>
	(make-instance of ACTION
		(phase	explosion)
		(location	?location)
		(object	chemical_reagent)
		(to-do	to_fight))
	(make-instance of EXPLAIN
		(title	explosion)
		(location	?location)
		(antec	2)
		(antec1	Chemical estinguishing station ?location is in fire)
		(antec2	Possibility of chemical reagent cylinders explosion)
		(consec	Set reagent on to fight in location ?location))
	(printout	t "   Предотвращение взрывов. Стравить баллоны c химическим реагентом в помещении " (upcase ?location) " во избежание взрыва." crlf))


;---------------------------------------------------------------------------------------------------------
; Изоляция горючих материалов					4 правила, 4 функции
;---------------------------------------------------------------------------------------------------------

(defmodule IMMEDIATE-ISOLATION
	(export	?ALL)
	(import	MAIN ?ALL))


(defrule	IMMEDIATE-ISOLATION::burning-machine-oil
	(or	(object	(is-a	LOCATION)
			(accedent	fire|threat)
			(burning	machine_oil)
			(title	?location))
		(object	(is-a	LOCATION)
			(evacuation to-evacuate|done)
			(burning	machine_oil)
			(title	?location)))
	(not	(object	(is-a	ACTION)
			(phase	isolation)
			(location	?location)
			(object	machine_oil)
			(to-do	pump_out|done)))
	=>
	(make-instance of ACTION
		(phase	isolation)
		(location	?location)
		(object	machine_oil)
		(to-do	pump_out))
	(make-instance of EXPLAIN
		(title	isolation)
		(location	?location)
		(antec	2)
		(antec1	Location ?location is in emergency)
		(antec2	Availability of combustible matherial: machine oil)
		(consec	Pump out machine oil from location ?location to prevent its ignition))
	(printout	t "   Изоляция горючих материалов. Откачать машинное масло из помещения "
		(upcase ?location) " во избежание возгорания." crlf))


(defrule	IMMEDIATE-ISOLATION::burning-working-clothes
	(or	(object	(is-a	LOCATION)
			(accedent	fire|threat)
			(burning	working_clothes)
			(title	?location))
		(object	(is-a	LOCATION)
			(evacuation to-evacuate|done)
			(burning	working_clothes)
			(title	?location)))
	(not	(object	(is-a	ACTION)
			(phase	isolation)
			(location	?location)
			(object	working_clothes)
			(to-do	carry_out|done)))
	=>
	(make-instance of ACTION
		(phase	isolation)
		(location	?location)
		(object	working_clothes)
		(to-do	carry_out))
	(make-instance of EXPLAIN
		(title	isolation)
		(location	?location)
		(antec	2)
		(antec1	Location ?location is in emergency)
		(antec2	Availability of combustible matherial: working clothes)
		(consec	Carry out working clothes from location ?location to prevent its ignition))
	(printout	t "   Изоляция горючих материалов. Вынести рабочую одежду из помещения "
		(upcase ?location) " во избежание возгорания." crlf))


(defrule	IMMEDIATE-ISOLATION::stop-machinery-emergency
	(or	(object	(is-a	LOCATION)
			(accedent	fire|threat)
			(machinery on)
			(title	?location))
		(object	(is-a	LOCATION)
			(evacuation to-evacuate|done)
			(machinery on)
			(title	?location)))
	=>
	(do-for-instance ((?l LOCATION))
		(eq	?l:title ?location)
		(send	?l put-machinery stop))
	(make-instance of EXPLAIN
		(title	isolation)
		(location	?location)
		(type	mech)
		(antec	2)
		(antec1	Location ?location is in emergency)
		(antec2	Machinery in location ?location is working)
		(consec	Stop machinery in location ?location to prevent ignition))
	(printout	t "   Изоляция горючих материалов. Вывести из действия механизмы в помещении "
		(upcase ?location) " во избежание их возгорания." crlf))


;--------------------------------------------------------------------------------------------------------
; Локализация очага пожара
;--------------------------------------------------------------------------------------------------------

(defmodule LOCALISATION
	(export	?ALL)
	(import	MAIN ?ALL))


(defrule	LOCALISATION::initiate
	(object	(is-a	LOCATION)
		(accedent	fire))
	=>
	(printout	t crlf "2. ЛОКАЛИЗАЦИЯ ОЧАГА ВОЗГОРАНИЯ:" crlf)
	(focus	ALLOCATION)
	(focus	FIRE-LINES))


;--------------------------------------------------------------------------------------------------------
; Определение рубежей обороны
;--------------------------------------------------------------------------------------------------------

(defmodule FIRE-LINES
	(export	?ALL)
	(import	MAIN ?ALL))


(defrule	FIRE-LINES::initiate
	(object	(is-a	LOCATION)
		(accedent	fire))
	=>
	(do-for-all-instances ((?b BORDER))
		(neq	?b:fire-line none)
		(send	?b put-fire-line none))
	(focus	FIRE-LINES-SEARCH)
	(printout	t "а) Расчет рубежей обороны:" crlf))


(defmodule FIRE-LINES-SEARCH
	(export	?ALL)
	(import	MAIN ?ALL))


(defrule	FIRE-LINES-SEARCH::places
	(object	(is-a	LOCATION)
		(accedent	fire)
		(title	?fire-location))
	(object	(is-a	BORDER)
		(from	?fire-location)
		(upon	?neighbour)
		(fire-line	none))
	(not	(or	(object	(is-a	LOCATION)
				(title	?neighbour)
				(accedent	fire))
			(object	(is-a	LOCATION)
				(title	?neighbour)
				(evacuation to-evacuate|done))))
	=>
	(do-for-instance ((?b BORDER))
		(and	(eq	?b:from ?fire-location)
			(eq	?b:upon ?neighbour))
		(send	?b put-fire-line line1))
	(printout	t "   Рубеж обороны. Переборка между помещениями " (upcase ?fire-location) " и " (upcase ?neighbour) "." crlf)
)


(defrule	FIRE-LINES::regroup
	(object	(is-a	BORDER)
		(fire-line	line1)
		(upon	?present-location))
;	(not	(fire-line-location	(location	?present-location)))
	=>
	(bind	?neighbours "")
	(bind	?length 0.0)
	(do-for-all-instances ((?b BORDER))
		(and	(eq	?b:upon ?present-location)
			(eq	?b:fire-line line1))
		(bind	?neighbours (str-cat ?neighbours ?b:from))
		(bind	?length (+ ?length ?b:length)))
	(bind	?hydrants (round (+ 0.5 (/ ?length 8))))
	(bind	?answer	(find-fact	((?fl fire-line-location))
				(eq	?fl:location ?present-location)))
	(if	(eq	1	(length$	?answer))
		then	(do-for-fact ((?fl fire-line-location))
				(eq	?fl:location ?present-location)
				(bind	?length-new (- ?length ?fl:perimeter))
				(bind	?hydrants-new (- ?hydrants ?fl:hydrants-here))
				(modify	?fl	(perimeter	?length)
						(hydrants-need	?hydrants-new))
				(printout	t "   Помещение " (upcase ?present-location) ". Периметр обороны увеличен на " ?length-new
					" м. Дополнительно требуется пожарных стволов: " ?hydrants-new " шт." crlf))
		else	(assert	(fire-line-location	(location		?present-location)
						(target		?neighbours)
						(perimeter	?length)
						(hydrants-need	?hydrants)))
			(printout	t "   Помещение " (upcase ?present-location) ". Периметр обороны: " ?length
				" м. Требуется пожарных стволов: " ?hydrants " шт." crlf))
)


(defrule	FIRE-LINES::co
	(declare	(salience	-10))
	(object	(is-a	LOCATION)
		(co	yes)
		(title	?fire-location)
		(accedent	?accedent)
		(evacuation ?evacuation))
	=>
	(if	(or	(eq	?evacuation to-evacuate)
			(eq	?evacuation done)
			(eq	?accedent threat))
		then	(printout	t "   Помещение " (upcase ?fire-location)
				". Рекомендуется применить систему ОХТ для предотвращения возгорания." crlf)))


;--------------------------------------------------------------------------------------------------------
; Подтягивание сил для изоляции
;--------------------------------------------------------------------------------------------------------

(defmodule ALLOCATION
	(export	?ALL)
	(import	MAIN ?ALL))


(defrule	ALLOCATION::initiate
	(declare	(salience	10))
	(object	(is-a	LOCATION)
		(accedent	fire))
	=>
	(printout	t "б) Расстановка сил и средств:" crlf)
	(focus	HYDRANTS-ALLOCATION-SELECTION)
	(focus	HYDRANTS-ALLOCATION)
	(focus	HYDRANTS-SEARCH))


(defmodule HYDRANTS-SEARCH
	(export	?ALL)
	(import	MAIN ?ALL))


(defrule	HYDRANTS-SEARCH::cut-off-hydrant-outs
	(declare	(salience	10))
	(or	(object	(is-a	LOCATION)
			(accedent	fire)
			(title	?fire-location))
		(object	(is-a	LOCATION)
			(evacuation to-evacuate|done)
			(title	?fire-location)))
	(hydrant-in-use	(location	?fire-location)
			(title	?hydrant))
	(fire-line-location	(hydrants-titles	$?titles&:(member$ ?hydrant $?titles))
			(location		?location)
			(hydrants-here	?here)
			(hydrants-need	?need))
	=>
	(do-for-all-facts	((?hu hydrant-in-use))
		(and	(eq	?hu:location ?fire-location)
			(neq	?hu:created auto))
		(retract	?hu))
	(do-for-fact	((?fl fire-line-location))
		(eq	?fl:location ?location)
		(bind	$?new (delete-member$ $?titles (explode$ (str-cat ?hydrant))))
		(modify	?fl	(hydrants-titles	$?new)
				(hydrants-here	(- ?here 1))
				(hydrants-need	(+ ?need 1)))))


(defrule	HYDRANTS-SEARCH::take-hydrant-outs
	(declare	(salience	10))
	(or	(object	(is-a	LOCATION)
			(accedent	fire)
			(title	?fire-location))
		(object	(is-a	LOCATION)
			(evacuation to-evacuate|done)
			(title	?fire-location)))
	?location <-	(fire-line-location	(location		?fire-location)
					(hydrants-titles	$?titles))
	=>
	(retract	?location)
	(loop-for-count	(?i (length$ $?titles))	do
		(do-for-instance ((?h HYDRANT))
			(eq	?h:title (nth$ ?i $?titles))
			(send	?h put-free (+ ?h:free 1))))
	(do-for-all-facts	((?hu hydrant-in-use))
		(and	(eq	?hu:root ?fire-location)
			(neq	?hu:created auto))
		(retract	?hu)))


(defrule	HYDRANTS-SEARCH::initiate
	(fire-line-location	(location		?fire-line-location)
			(hydrants-need	?need&:(neq ?need 0)))
	=>
	(assert	(hydrants-search-progress	(location	?fire-line-location)))
	(assert	(check-for-hydrant	(what	?fire-line-location)
				(root	?fire-line-location)
				(past	"")
				(path	"")
				(distance	3.5)
				(number	(count-hydrants ?fire-line-location))))
;	(printout	t crlf "Начат поиск гидрантов для помещения " ?fire-line-location "." crlf)
)


(defrule	HYDRANTS-SEARCH::move
	?move <- (move-hydrant	(present	?present-location)
				(root	?root)
				(past	?past-location)
				(distance	?old-distance)
				(path	?path))
	(or	(object	(is-a	DOOR)
			(from	?present-location)
			(to	?check-location&~out&~?root))
		(object	(is-a	DOOR)
			(to	?present-location)
			(from	?check-location&~out&~?root)))
	(test	(not	(str-index ?check-location ?path)))
	=>
	(bind	?back (arrange-letters ?present-location ?past-location))
	(bind	?forward (arrange-letters ?present-location ?check-location))
	(bind	?add-distance 0.0)
	(do-for-all-instances ((?fd FIRE-DISTANCE))
		(or	(and	(eq	?fd:from ?back)
				(eq	?fd:to ?forward))
			(and	(eq	?fd:from ?forward)
				(eq	?fd:to ?back)))
		(bind	?add-distance (+ ?add-distance ?fd:value)))
	(bind	?full-distance (+ ?old-distance ?add-distance))
;	(printout	t "Добавлено расстояние " ?old-distance "+" ?add-distance "=" ?full-distance "."
;		" Расстояние от " ?back " до " ?forward "." crlf)
	(if	(not	(str-index ?check-location (get-emergent)))
		then	(bind	?number (count-hydrants ?check-location))
			;(retract	?move)
			(assert	(check-for-hydrant	(what	?check-location)
						(root	?root)
						(past	?present-location)
						(path	?path)
						(distance	?full-distance)
						(number	?number)))
;			(printout	t "Поиск перемещен из " ?present-location " в помещение " ?check-location "." crlf)
	))


(defrule	HYDRANTS-SEARCH::none
	(or	?check <- (check-for-hydrant	(what	?present-location)
					(number	0)
					(root	?root)
					(past	?past-location)
					(path	?path)
					(distance	?old-distance))
		(and	?check <- (check-for-hydrant	(what	?present-location)
					(number	?number&:(neq ?number 0))
					(root	?root)
					(past	?past-location)
					(path	?path)
					(checked	?checked)
					(distance	?old-distance))
			(object	(is-a	HYDRANT)
				(location	?present-location)
				(title	?hydrant)
				(free	?free&:(eq ?free 0)))
			(test	(not	(str-index ?hydrant ?checked)))))
	(or	(object	(is-a	DOOR)
			(from	?present-location)
			(to	?check-location&~out))
		(object	(is-a	DOOR)
			(to	?present-location)
			(from	?check-location&~out)))
	(test	(not	(str-index ?check-location ?path)))
	=>
	(retract	?check)
	(assert	(move-hydrant	(root	?root)
				(past	?past-location)
				(present	?present-location)
				(path	(sym-cat	?path ?present-location))
				(distance	?old-distance)))
;	(printout	t "    Нет гидрантов в помещении " ?present-location "." crlf)
)


(defrule	HYDRANTS-SEARCH::exist
	?check <- (check-for-hydrant	(what	?present-location)
				(root	?root)
				(past	?past-location)
				(path	?path)
				(distance	?old-distance)
				(checked	?checked)
				(number	?number&:(neq ?number 0)))
	(object	(is-a	HYDRANT)
		(location	?present-location)
		(title	?hydrant)
		(free	?free&:(neq ?free 0)))
	(test	(not	(str-index ?hydrant ?checked)))
	=>
	(bind	?back (arrange-letters ?present-location ?past-location))
	(bind	?add-distance 0.0)
	(do-for-all-instances ((?fd FIRE-DISTANCE))
		(and	(eq	?fd:from (create$ ?hydrant))
			(eq	?fd:to ?back))
		(bind	?add-distance (+ ?add-distance ?fd:value)))
	(bind	?full-distance (+ ?old-distance ?add-distance))
	(if	(<	?full-distance 20.0)
		then	(assert	(hydrant-potential	(title	?hydrant)
						(distance	?full-distance)
						(location	?present-location)
						(path	(sym-cat	?path ?present-location))
						(root	?root)))
;			(printout	t "    Потенциальный гидрант " ?hydrant ", " (+ ?old-distance ?add-distance)
;				", root: " ?root ", path: " ?path ", " present: ?present-location "." crlf)
		else	(assert	(hydrant-reserve	(title	?hydrant)
						(distance	?full-distance)
						(location	?present-location)
						(path	(sym-cat	?path ?present-location))
						(root	?root)))
;			(printout	t "    Резервный гидрант " ?hydrant ", " (+ ?old-distance ?add-distance)
;				", root: " ?root ", path: " ?path ", " present: ?present-location "." crlf)
	)
	(if	(eq	?number 1)
		then	(retract	?check)
			(assert	(move-hydrant	(root	?root)
						(past	?past-location)
						(present	?present-location)
						(path	(sym-cat	?path ?present-location))
						(distance	?old-distance)))
		else	(bind	?number (- ?number 1))
			(modify	?check (number ?number) (checked (str-cat ?checked ?hydrant)))))


(defrule	HYDRANTS-SEARCH::burn-garbage
	(declare	(salience	-10))
	(or	?remove <- (move-hydrant)
		?remove <- (check-for-hydrant))
	=>
	(retract	?remove))


(defmodule HYDRANTS-ALLOCATION
	(export	?ALL)
	(import	MAIN ?ALL))


(defrule	HYDRANTS-ALLOCATION::burn-empty
	(declare	(salience	10))
	?progress <-	(hydrants-search-progress	(location	?fire-line-location))
	(not	(hydrant-potential	(root	?fire-line-location)))
	=>
	(retract	?progress))


(defrule	HYDRANTS-ALLOCATION::set-weight
	(declare	(salience	10))
	(or	(hydrant-potential	(title	?hydrant)
				(distance	?full-distance)
				(path	?path)
				(root	?root)
				(weight-p	0)
				(weight-n	0))
		(hydrant-reserve	(title	?hydrant)
				(distance	?full-distance)
				(path	?path)
				(root	?root)
				(weight-p	0)
				(weight-n	0))
		(hydrant-shadow	(title	?hydrant)
				(distance	?full-distance)
				(path	?path)
				(root	?root)
				(weight-p	0)
				(weight-n	0)))
	=>
	(bind	?weight-positive (count-weight-positive ?root))
	(bind	?weight-negative (count-weight-negative (str-length ?path) ?full-distance))
	(do-for-fact ((?hp hydrant-potential))
		(and	(eq	?hp:title ?hydrant)
			(eq	?hp:path ?path))
		(modify	?hp	(weight-p	?weight-positive)
				(weight-n	?weight-negative)))
	(do-for-fact ((?hr hydrant-reserve))
		(and	(eq	?hr:title ?hydrant)
			(eq	?hr:path ?path))
		(modify	?hr	(weight-p	?weight-positive)
				(weight-n	?weight-negative)))
	(do-for-fact ((?hs hydrant-shadow))
		(and	(eq	?hs:title ?hydrant)
			(eq	?hs:path ?path))
		(modify	?hs	(weight-p	?weight-positive)
				(weight-n	?weight-negative))))


(defrule	HYDRANTS-ALLOCATION::allocate
	?allocate <-	(allocate	(location	?fire-line-location)
				(hydrant	?hydrant))
	?progress	<-	(hydrants-search-progress	(location	?fire-line-location))
	?fire-line	<-	(fire-line-location	(location	?fire-line-location)
					(target	?target)
					(hydrants-here 	?here)
					(hydrants-need	?need)
					(hydrants-titles	$?titles))
	?potential <-	(hydrant-potential	(root	?fire-line-location)
					(title	?hydrant)
					(location	?hydrant-location)
					(distance	?distance)
					(path	?path)
					(weight-n	?wn))
	=>
	(bind	?number-to-take 1)
	(do-for-instance ((?h HYDRANT))
		(eq	?h:title ?hydrant)
		(if	(>=	?need ?h:free)
			then	(bind	?number-to-take ?h:free))
		(if	(<=	?h:free ?number-to-take)
			then	(retract	?potential))
		(send	?h put-free (- ?h:free ?number-to-take)))
	(bind	$?full-titles (create$ ?hydrant $?titles))
	(loop-for-count	(?i (- ?number-to-take 1))	do
		(bind	$?full-titles (create$ ?hydrant $?full-titles)))
	(loop-for-count	(?i ?number-to-take)	do
		(assert	(hydrant-in-use	(root	?fire-line-location)
					(location	?hydrant-location)
					(title	?hydrant)
					(distance	?distance)
					(path	?path)
					(weight-n	?wn)
					(id	(gensym*)))))
	(modify	?fire-line	(hydrants-here 	(+	?here ?number-to-take))
			(hydrants-titles	$?full-titles)
			(hydrants-need	(-	?need ?number-to-take)))
	(retract	?progress)
	(printout	t "   Помещение " (upcase ?fire-line-location)
		". Установить наблюдение за переборкой " (upcase ?fire-line-location) "-" (upcase ?target)
		". Протянуть пожарный рукав: \"" (implode$ $?full-titles) "\". Длина рукава: "?distance ". Путь: " (upcase ?path) ".")
	(if	(neq	?need 1)
		then	(printout	t " Стволов: " ?number-to-take " шт." crlf)
		else	(printout	t crlf))
	(if	(or	(eq	?need ?number-to-take)
			(eq	?need 1))
		then	(do-for-all-facts ((?hp hydrant-potential))
				(eq	?hp:root ?fire-line-location)
				(retract	?hp)
				(assert	(hydrant-shadow	(root	?hp:root)
							(title	?hp:title)
							(location	?hp:location)
							(distance	?hp:distance)
							(path	?hp:path))))
		else	(modify	?progress	(location	?fire-line-location)))
	(retract	?allocate))


(defrule	HYDRANTS-ALLOCATION::allocate-reserve
	?allocate <-	(allocate	(location	?fire-line-location)
				(hydrant	?hydrant))
	(not	(hydrants-search-progress	(location	?fire-line-location)))
	?fire-line	<-	(fire-line-location	(location	?fire-line-location)
					(target	?target)
					(hydrants-here 	?here)
					(hydrants-need	?need)
					(hydrants-titles	$?titles))
	?reserve <-	(hydrant-reserve	(root	?fire-line-location)
					(title	?hydrant)
					(location	?hydrant-location)
					(distance	?distance)
					(path	?path)
					(weight-n	?wn))
	=>
	(bind	?number 0)
	(bind	$?full-titles (create$ ?hydrant $?titles))
	(do-for-instance ((?h HYDRANT))
		(eq	?h:title ?hydrant)
		(bind	?number	?h:free))
	(if	(neq	?number 0)
		then	(retract	?reserve)
			(do-for-instance ((?h HYDRANT))
				(eq	?h:title ?hydrant)
				(send	?h put-free (- ?h:free 1)))
			(modify	?fire-line	(hydrants-here 	(+	?here 1))
					(hydrants-titles	$?full-titles)
					(hydrants-need	(-	?need 1))))
	(if	(eq	?need 1)
		then	(printout	t "   Помещение " (upcase ?fire-line-location)
				". Установить наблюдение за переборкой " (upcase ?fire-line-location) "-" (upcase ?target)
				". Протянуть пожарный рукав: \"" (implode$ $?full-titles)
				"\". Длина рукава: "?distance ". Путь: " (upcase ?path) ". [резерв]" crlf)
			(assert	(hydrant-in-use	(root	?fire-line-location)
						(location	?hydrant-location)
						(title	?hydrant)
						(distance	?distance)
						(path	?path)
						(weight-n	?wn)
						(id	(gensym*))))
			(do-for-all-facts ((?hr hydrant-reserve))
				(eq	?hr:root ?fire-line-location)
				(retract	?hr)))
	(retract	?allocate))


(defrule	HYDRANTS-ALLOCATION::keep-open-doors
	(hydrant-in-use	(root	?root)
			(title	?title)
			(path	?path&:(> (str-length ?path) 1)))
	=>
	(loop-for-count	(?i (- (str-length ?path) 1))	do
		(bind	?door (arrange-letters (sub-string ?i ?i ?path) (sub-string (+ ?i 1) (+ ?i 1) ?path)))
		(do-for-instance	((?d DOOR))
			(and	(eq	?d:from	(nth$ 1 ?door))
				(eq	?d:to	(nth$ 2 ?door))
				(or	(eq	?d:status to-close)
					(eq	?d:status closed))
				(neq	?d:status	keep-open))
			(send	?d put-status keep-open)
			(printout	t "   Герметизация. Отдраить дверь из " (upcase (nth$ 1 ?door))
				" в " (upcase (nth$ 2 ?door)) ". Будет протянут пожарный рукав." crlf))))


(defrule	HYDRANTS-ALLOCATION::keep-hydrant-outs-integrity
	(object	(is-a	HYDRANT)
		(free	0)
		(title	?hydrant))
	=>
	(bind	?potential-root "")
	(do-for-all-facts ((?hp hydrant-potential))
		(eq	?hp:title ?hydrant)
		(retract	?hp)
		(bind	?potential-root ?hp:root))
	(delayed-do-for-all-facts ((?hp hydrant-potential))
		(neq	?hp:title ?hydrant)
		(modify	?hp (weight-p (count-weight-positive ?hp:root))))
	(do-for-all-facts ((?hr hydrant-reserve))
		(eq	?hr:title ?hydrant)
		(retract	?hr))
	(do-for-all-facts ((?hs hydrant-shadow))
		(eq	?hs:title ?hydrant)
		(retract	?hs))
	(do-for-all-facts ((?hp hydrant-positive))
		(eq	?hp:title ?hydrant)
		(retract	?hp)))


(defmodule HYDRANTS-ALLOCATION-SELECTION
	(export	?ALL)
	(import	MAIN ?ALL))


;; KNOWN LIMITATION (not a regression, pre-existing): begin-positive/begin-negative below only
;; compare hydrant-potential/hydrant-positive facts sharing the same ?root (one fire-line location's
;; own search) - there is no arbitration ACROSS different ?root locations competing for the same
;; scarce HYDRANT outlet. Whichever location's activation happens to fire first on CLIPS's agenda
;; wins the outlet, even if another competing location's own weight for that same hydrant is
;; objectively better. Reproduced on scenario K: room "m" wants hydr_m at weight-n=13.5 (its own
;; local hydrant - clearly the best possible claim), yet room "g" (weight-n=36.3 for the same
;; hydrant) can still claim the outlet first, leaving "m" to fall back to a worse one. This was
;; already true before any Java-driven instance seeding (the rules here are unchanged); it was just
;; as arbitrary before, governed by feis.clp's own hardcoded fact order instead. Left as a known
;; limitation for a future improvement, not fixed here.
(defrule	HYDRANTS-ALLOCATION-SELECTION::begin-positive
	(declare	(salience	10))
	(hydrant-potential	(weight-p	?weight-p)
			(root	?fire-line-location)
			(title	?hydrant)
			(weight-n	?negative))
	(not	(hydrant-potential	(weight-p	?wp&:(> ?wp ?weight-p))))
	(hydrants-search-progress	(location	?fire-line-location))
	=>
	(assert	(hydrant-positive	(title	?hydrant)
				(root	?fire-line-location)
				(negative	?negative))))


(defrule	HYDRANTS-ALLOCATION-SELECTION::begin-negative
	?positive <-	(hydrant-positive	(title	?hydrant)
					(root	?fire-line-location)
					(negative	?weight-n))
	(not	(hydrant-positive	(negative	?wn&:(< ?wn ?weight-n))))
	(hydrants-search-progress	(location	?fire-line-location))
	=>
	(assert	(allocate	(location	?fire-line-location)
			(hydrant	?hydrant)))
	(do-for-all-facts ((?hp hydrant-positive))
		(neq	?hp:title ?hydrant)
		(retract	?hp))
	(retract	?positive)
	(focus	HYDRANTS-ALLOCATION))


(defrule	HYDRANTS-ALLOCATION-SELECTION::begin-negative-just-one
	(declare	(salience	-10))
	?positive <-	(hydrant-positive	(title	?hydrant)
					(root	?fire-line-location))
	(hydrants-search-progress	(location	?fire-line-location))
	=>
	(bind	?number-of-facts 0)
	(do-for-all-facts ((?hp hydrant-positive))
		(eq	?hp:root ?fire-line-location)
		(bind	?number-of-facts (+ 1 ?number-of-facts)))
	(if	(eq	1 ?number-of-facts)
		then	(assert	(allocate	(location	?fire-line-location)
					(hydrant	?hydrant)))
			(do-for-all-facts ((?hp hydrant-positive))
				(neq	?hp:title ?hydrant)
				(retract	?hp))
			(retract	?positive)
			(focus	HYDRANTS-ALLOCATION)))


(defrule	HYDRANTS-ALLOCATION::begin-reserve
	(declare	(salience	-10))
	(fire-line-location	(hydrants-need	?need&:(neq ?need 0))
			(location		?fire-line-location))
	(not	(hydrants-search-progress	(location	?fire-line-location)))
	=>
	(do-for-all-facts ((?hr hydrant-reserve))
		(eq	?hr:root ?fire-line-location)
		(assert	(hydrant-positive	(title	?hr:title)
					(root	?fire-line-location)
					(negative	?hr:weight-n))))
	(focus	ALLOCATION-RESERVE))


(defmodule ALLOCATION-RESERVE
	(export	?ALL)
	(import	MAIN ?ALL))


(defrule	ALLOCATION-RESERVE::begin-negative
	?positive <-	(hydrant-positive	(title	?hydrant)
					(root	?fire-line-location)
					(negative	?weight-n))
	(not	(hydrant-positive	(negative	?wn&:(< ?wn ?weight-n))))
	=>
	(assert	(allocate	(location	?fire-line-location)
			(hydrant	?hydrant)))
	(do-for-all-facts ((?hp hydrant-positive))
		(neq	?hp:title ?hydrant)
		(retract	?hp))
	(retract	?positive)
	(focus	HYDRANTS-ALLOCATION))


(defmodule EXTINGUISHING
	(export	?ALL)
	(import	MAIN ?ALL))


(defrule	EXTINGUISHING::initiate
	(object	(is-a	LOCATION)
		(accedent	fire))
	=>
	(printout	t crlf "3. ТУШЕНИЕ ПОЖАРА" crlf)
	(focus	MAIN)
;	(focus	EXTINGUISHING-COMPLETE)
	(focus	EXTINGUISHING-PLAN)
	(focus	EXTINGUISHING-GRAPH)
	(focus	EXTINGUISHING-CLEAR)
)


(defmodule EXTINGUISHING-CLEAR
	(export	?ALL)
	(import	MAIN ?ALL))


;(defrule	EXTINGUISHING-CLEAR::clear-array
;	?array <-	(array)
;	=>
;	(retract	?array))


(defrule	EXTINGUISHING-CLEAR::clear-plan
	?plan <-	(plan	(location	?location)
			(from	?from))
	=>
	(do-for-fact ((?eg ext-graph))
		(and	(eq	?eg:from	?from)
			(eq	?eg:to	?location))
		(modify	?eg	(from	?eg:from)))
	(do-for-fact ((?ee ext-edge))
		(eq	?ee:location	?location)
		(modify	?ee	(location	?ee:location)))
	(retract	?plan))


(defrule	EXTINGUISHING-CLEAR::clear-number
	?number <- (plan-number)
	=>
	(retract	?number))


(defmodule EXTINGUISHING-GRAPH
	(export	?ALL)
	(import	MAIN ?ALL))


(defrule	EXTINGUISHING-GRAPH::make-graph-marginal
	(declare	(salience	20))
	(object	(is-a	LOCATION)
		(accedent	fire)
		(title	?fire-location)
		(area	?area))
	(object	(is-a	LOCATION)
		(accedent	threat)
		(evacuation none)
		(title	?threat-location))
	(or	(object	(is-a	DOOR)
			(from	?fire-location)
			(to	?threat-location))
		(object	(is-a	DOOR)
			(from	?threat-location)
			(to	?fire-location)))
	=>
	(assert	(ext-graph (from	?threat-location)
			(to	?fire-location)))
	(assert	(ext-edge	(location	?fire-location)
			(to-needs	(count-to-need ?area))))
	(printout	t "   Добавлено в граф: дверь из " ?threat-location " в " ?fire-location "." crlf)
)


(defrule	EXTINGUISHING-GRAPH::make-graph-emergent
	(declare	(salience	10))
	(ext-edge	(location	?fire-location))
	(or	(object	(is-a	DOOR)
			(from	?fire-location)
			(to	?deeper-fire-location))
		(object	(is-a	DOOR)
			(from	?deeper-fire-location)
			(to	?fire-location)))
	(object	(is-a	LOCATION)
		(title	?deeper-fire-location)
		(accedent	fire)
		(area	?area))
	(not	(or	(ext-graph (from	?fire-location)
				(to	?deeper-fire-location))
			(ext-graph (from	?deeper-fire-location)
				(to	?fire-location))))
	=>
	(assert	(ext-graph (from	?fire-location)
			(to	?deeper-fire-location)))
	(assert	(ext-edge	(location	?deeper-fire-location)
			(to-needs	(count-to-need ?area))))
	(printout	t "   Добавлено в граф: дверь из " ?fire-location " в " ?deeper-fire-location "." crlf)
)


(defrule	EXTINGUISHING-GRAPH::by-border
	(ext-graph (to	?fire-location)
		(from	?location))
	?edge <-	(ext-edge	(location	?fire-location)
			(to-needs	?need)
			(by-border 0))
	=>
	(bind	?accessible 0)
	(do-for-all-facts ((?eg ext-graph))
		(eq	?eg:to ?fire-location)
		(do-for-instance ((?b BORDER))
			(or	(and	(eq	?b:from ?eg:from)
					(eq	?b:upon ?eg:to))
				(and	(eq	?b:from ?eg:to)
					(eq	?b:upon ?eg:from)))
			(bind	?to-add (round (+ 0.5 (/ ?b:length 8))))
			(bind	?accessible (+ ?accessible ?to-add))))
	(modify	?edge	(by-border ?accessible)))


(defrule	EXTINGUISHING-GRAPH::start-arrays
	(ext-graph (from	?root-location)
		(to	?fire-location))
	(object	(is-a	LOCATION)
		(title	?root-location)
		(accedent	threat))
	=>
	(assert	(ext-array (letters	(create$	?root-location ?fire-location))
			(index	1)))
;	(printout	t "   Добавлен массив: 0 - 1 - " ?root-location ?fire-location crlf)
)


(defrule	EXTINGUISHING-GRAPH::fill-arrays
	(ext-array 	(checked	no)
			(letters	$?letters)
			(index	?index))
	=>
	(bind	?number 1)
	(do-for-all-facts ((?eg ext-graph))
		(eq	?eg:from (nth$ 2 $?letters))
		(bind	$?new-letters (create$ ?eg:from ?eg:to))
		(bind	?new-index (+ ?number (* ?index 10)))
		(assert	(ext-array (letters	$?new-letters)
				(index	?new-index)))
;		(printout	t "   Добавлен массив: " ?new-depth " - " ?new-index " - " $?new-letters crlf)
		(bind	?number (+ ?number 1))))


(defrule	EXTINGUISHING-GRAPH::decompose-arrays
	(declare	(salience	-10))
	?array <-	(ext-array	(checked	no)
				(letters	$?letters)
				(branch	$?branch)
				(price	?price)
				(index	?index))
	(not	(ext-array	(checked	no)
				(index	?comp-index&:(> ?comp-index ?index))))
	=>
	(bind	?add-price 1)
	(do-for-instance ((?l LOCATION))
		(eq	?l:title (nth$ 2 $?letters))
		(if	(eq	?l:type engine-room)
			then	(bind	?add-price 4))
		(if	(eq	?l:type post)
			then	(bind	?add-price 3))
		(if	(or	(eq	?l:type service)
				(eq	?l:type auxilary))
			then	(bind	?add-price 2)))
	(do-for-all-facts ((?ea ext-array))
		(and	(eq	(nth$ 2 $?ea:letters) (nth$ 1 $?letters))
			(not	(member$ (nth$ 2 $?letters) $?ea:branch)))
		(modify	?ea	(price	(+ ?ea:price ?price ?add-price))
				(branch	(create$	$?ea:branch (nth$ 2 $?letters)))
				(letters	(create$	$?ea:letters (nth$ 2 $?letters) $?branch))))
	(modify	?array	(price	(+ ?price ?add-price))
			(checked	yes))
)


(defrule	EXTINGUISHING-GRAPH::delete-running
	(declare	(salience	-20))
	?array <-	(ext-array	(letters	$?letters)
				(branch	$?branch)
				(index	?index))
	(not	(ext-array	(checked	no)
				(index	?comp-index&:(> ?comp-index ?index))))
	=>
	(if	(eq	0 (length$ $?branch))
		then	(do-for-all-facts ((?ea ext-array))
				(and	(eq	(nth$ 2 $?ea:letters) (nth$ 1 $?letters))
					(eq	1 (length$ $?ea:branch)))
				(modify	?ea	(branch))
				(retract	?array))))


(defmodule EXTINGUISHING-PLAN
	(export	?ALL)
	(import	MAIN ?ALL))


(defrule	EXTINGUISHING-PLAN::create-numbers
	(declare	(salience	10))
	(ext-array (index	1)
		(letters	$?letters))
	=>
	(assert	(plan-number	(locations		(rest$ $?letters))
				(last-number	1))))


(defrule	EXTINGUISHING-PLAN::create-plans
	?array <-	(ext-array	(letters	$?letters)
				(branch	$?branch)
				(price	?price)
				(index	?index))
	(not	(ext-array	(price	?comp-price&:(> ?comp-price ?price))))
	=>
	(bind	?length (length$ $?letters))
	(bind	?number 0)
	(do-for-fact ((?pn plan-number))
		(member$ (nth$ 1 $?letters) $?pn:locations)
		(bind	?number ?pn:last-number)
		(modify	?pn	(last-number (+ 1 ?pn:last-number))))
	(loop-for-count	(?i	2 ?length)
		(if	(or	(not	(member$ (nth$ ?i $?letters) $?branch))
				(eq	?i ?length))
			then	(bind	?number (+ 1 ?number))
				(assert	(plan	(number	?number)
						(location	(nth$ ?i $?letters))
						(from	(nth$ (- ?i 1) $?letters))))
				(if	(and	(eq	0 (length$ $?branch))
						(eq	?i ?length))
					then	(retract	?array)
						(break))
			else	(retract	?array)
				(break))))


;(defrule	EXTINGUISHING-PLAN::resolve-collisions
;	(plan	(location	?location)
;		(number	?number-1)
;		(from	?from-1))
;	(plan	(location	?location)
;		(number	?number-2)
;		(from	?from-2))
;	=>
;	(bind	?price)
;	(do-for-all-facts (?p plan)
;		(eq	?p:location ?location)








