grammar MembraneModel;

// The whole file
model
    : line+
    ;

// One line in the file
line 
    :  initialTreeDefinition
    |  speciesDefinition
    |  parameterDefinition
    |  reactionDefinition
    |  locationContextBlock
    |  NEWLINE
    ;

// The initial conditions of nested membranes and species
initialTreeDefinition
    : 'initial tree' NEWLINE* '{' tagsList=tags? simulationName=IDENTIFIER initialMembraneDefinition* '}' NEWLINE*
    ;

// One level of the tree of initial conditions
initialMembraneDefinition
    : NEWLINE* '{' howMany=INTEGER? tagsList=tags? membraneName=IDENTIFIER initialMembraneDefinition* '}' NEWLINE*
    ;

// Definition of a species
// Location and membrane are optional only if this definition is within a locationContextBlock
speciesDefinition 
    :  'species' name=IDENTIFIER (location=LOCATION membrane=IDENTIFIER)? '=' howMany=equation speciesUnits=units tagsList=tags?
    ;

// Definition of a parameter
parameterDefinition
    : 'parameter' name=IDENTIFIER '=' value=equation parameterUnits=units
    ;

// A location context block
locationContextBlock
	:  locationContextBlockStart '{' 
		(  speciesDefinition
    	|  parameterDefinition
    	|  reactionDefinition
    	|  NEWLINE
		)* '}'
	;
	
locationContextBlockStart
	:  location=LOCATION membrane=IDENTIFIER
	;

// List of tags
tags
	: '[' tag+ ']'
	;

// An individual tag
tag
	: value=(IDENTIFIER | IDENTIFIER_QUOTED)
	;

// A whole reaction
reactionDefinition
    :  'reaction'? 
       combinedSpecies 
       keyword_modifiers+='modifier' forwardModifier=equation
       (keyword_delays+='delay' forwardDelay=equation)?
       reactionType=arrow 
       (  combinedSpecies 
       	  (keyword_modifiers+='modifier' reverseModifier=equation)?
       	  (keyword_delays+='delay' reverseDelay=equation)?
       |  'decay'	
       )
    ;

arrow
	: ARROW
	;

// Compound units
units
    :  unit ('.' unit)*
    ;

// One unit, e.g. "hour^-1"
unit
    :  IDENTIFIER ( '^' '-'? INTEGER )?
    ;

// Multiple species in locations, combined with "binds" or "and" 
combinedSpecies
    :  stoichiometrySpecies (combiners+=COMBINER stoichiometrySpecies)*
    ;

// A species in a location, with stoichiometry information
stoichiometrySpecies
    :  stoich=stoichiometry? locSpecies=locatedSpecies
    ;

// A species in a location, e.g. "INFg around macrophage"
// Location and membrane are optional only if this locatedSpecies is within a locationContextBlock
locatedSpecies
    :  species=IDENTIFIER (location=LOCATION membrane=IDENTIFIER)?
    ;

// Equations involving identifiers and numbers
equation
    : 'round' '(' expression=equation ')'                   #roundExpression
    | 'ln' '(' expression=equation ')'                      #naturalLogExpression
    | '(' expression=equation ')'                           #parenExpression
    |  '-' expression=equation                              #minusExpression
    |  left=equation operation=( '*' | '/' ) right=equation #operationExpression
    |  left=equation operation=( '+' | '-' ) right=equation #operationExpression
    |  number=( FLOAT | INTEGER )                           #numberExpression
    |  parameterOrSpecies=IDENTIFIER                        #parameterOrSpeciesExpression  // Could be a species in a locationContextBlock
    |  locSpecies=locatedSpecies                            #locatedSpeciesExpression  // Will always contain location information, because otherwise parameterOrSpeciesExpression would match first
    ;

// The number of units of a species that are consumed/produced in a reaction
stoichiometry
    :  'gene' | 'catalyst' | INTEGER
    ;

LOCATION
    :  'on' | 'under' | 'contained' | 'around'
    ;

COMBINER
    :  'binds' | 'and'
    ;

ARROW
    :  '<=>' | '=>'
    ;

FLOAT
	:  (INTEGER EXPONENT)   // An integer with scientific notation counts as a float
	|  (DECIMAL EXPONENT?)  // Decimal numbers count as floats, and the scientific notation is optional
	;

INTEGER
    :  DIGIT+
    ;
    
fragment DECIMAL
	:  DIGIT+ '.' DIGIT+
	;

fragment EXPONENT
	:  ('e' | 'E') ('+' | '-') DIGIT+
	;

// An identifier that can contain spaces
IDENTIFIER_QUOTED
	: '\'' ( LETTER | DIGIT )+ (( LETTER | DIGIT | '-' | '_' | ' ')* ( LETTER | DIGIT | '-' | '_' ))? '\''
	;

IDENTIFIER
    :  ( LETTER | DIGIT )+ ( LETTER | DIGIT | '-' | '_' )*
    ;

fragment DIGIT 
    :  '0' .. '9'
    ;

fragment LETTER
    :  'a'..'z' | 'A'..'Z'
    ;

COMMENT 
    :  '#' ~( '\r' | '\n' )* -> skip
    ;

WHITESPACE
    :  (' ' | '\t')+ -> skip
    ;

NEWLINE
    :  '\r'? '\n'
    ;
