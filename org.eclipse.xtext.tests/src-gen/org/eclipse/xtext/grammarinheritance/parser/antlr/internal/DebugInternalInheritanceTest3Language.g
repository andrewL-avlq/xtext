/*
 * generated by Xtext
 */
grammar DebugInternalInheritanceTest3Language;

// Rule Model
ruleModel:
	superModel
;

// Rule Element
ruleElement:
	(
		superElement
		    |
		'element'
		SUPER_ID
		    |
		'element'
		SUPER_1_ID
		    |
		'element'
		RULE_STRING
	)
;

// Rule Model
superModel:
	'model'
	RULE_ID
	'{'
	ruleElement
	*
	'}'
;

// Rule Element
superElement:
	'element'
	RULE_ID
;

RULE_ID : 'id';

SUPER_ID : ('a'..'z')+;

SUPER_1_ID : '^'? ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')*;

RULE_INT : ('0'..'9')+;

RULE_STRING : ('"' ('\\' .|~(('\\'|'"')))* '"'|'\'' ('\\' .|~(('\\'|'\'')))* '\'');

RULE_ML_COMMENT : '/*' ( options {greedy=false;} : . )*'*/' {skip();};

RULE_SL_COMMENT : '//' ~(('\n'|'\r'))* ('\r'? '\n')? {skip();};

RULE_WS : (' '|'\t'|'\r'|'\n')+ {skip();};

RULE_ANY_OTHER : .;
