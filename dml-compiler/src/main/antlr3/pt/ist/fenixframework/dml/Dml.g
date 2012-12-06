grammar Dml;

options {
    output=AST;
    ASTLabelType=CommonTree;
	backtrack=true;
	memoize=true;
}

tokens {
	SLOT; ROLE; ROLE; MULTIPLICITY; INDEXED; METADATA; STRING; NUMBER; FIELD; ARRAY;
    TRUE; FALSE; NULL;
}

@parser::header {
package pt.ist.fenixframework.dml;

import java.util.regex.Pattern;
}

@lexer::header {
package pt.ist.fenixframework.dml;
}

compilationUnit
    :   definitions* EOF!
    ;

definitions
    :   packageDeclaration
    |   enumType
    |   valueType
    |   externalDeclaration
    |   classDeclaration
    |   relation
    ;

packageDeclaration
    :   'package'^ qualifiedName ';'!
    ;


enumType
    :   'enum'^ qualifiedName ('as'! qualifiedName)? ';'!
    ;

valueType
    :   'valueType'^ type ('as'! qualifiedName)? valueTypeBody
    ;

valueTypeBody
    :   '{'! externalizationClause (internalizationClause)? '}'!
    ;

externalizationClause
    :   'externalizeWith'^ '{'! externalizationElement+'}'!
    ;

externalizationElement
    :   qualifiedName Identifier '('! ')'! ';'!
    ;

internalizationClause
    :   'internalizeWith'^ Identifier '('! ')'! ';'!
    ;

externalDeclaration
    :   'external'^ 'class'! Identifier ('as' Identifier)? ';'
    ;

classDeclaration
    :   'class' name=entityTypeIdentifier
        ('extends' sup=entityTypeIdentifier)?
        ('implements' imp+=entityTypeIdentifier (',' imp+=entityTypeIdentifier)*)?
        classBody
    ->
    	^('class' $name ^('extends' $sup)? ^('implements' $imp+)? classBody)
    ;

classBody
    :   '{'! slotDeclaration* '}'!
    |   ';'!
    ;

slotDeclaration
    :   slotType=type name=Identifier metadata? ';'
    ->  ^(SLOT $slotType $name metadata?)
    ;

relation
    :   'relation'^ Identifier '{'! role* '}'!
    ;

role
    :   roleType=entityTypeIdentifier 'playsRole' roleName=Identifier? multiplicity? indexed? 'ordered'? metadata? ';'
    ->  ^(ROLE $roleType $roleName? multiplicity? indexed? metadata?)
    ;

multiplicity
    :   'multiplicity' range
    ->  ^(MULTIPLICITY range)
    ;

indexed
    :   'indexed' 'by' Identifier
    ->  ^(INDEXED Identifier)
    ;

range
    :   Digit+ '..' (Digit+ | '*')
    |   (Digit+ | '*')
    ;

entityTypeIdentifier
    :   qualifiedName
    |   '.' qualifiedName
    ;

type
	:	qualifiedName typeArguments? ('[' ']')*
	;

typeArguments
    :   '<' typeArgument (',' typeArgument)* '>'
    ;

typeArgument
    :   type
    |   '?' (('extends' | 'super') type)?
    ;

qualifiedName
    :   Identifier ('.' Identifier)*
    ;

/* Json metadata */

metadata
    :   '{' (pairs+=jsonPair (',' pairs+=jsonPair)*)? '}'
    ->  ^(METADATA $pairs+)
    ;
 
jsonPair
    :   name=Identifier ':' value=jsonValue
    ->  ^(FIELD $name $value)
    ;

jsonValue
    :   string
    |   number
    |   metadata
    |   jsonArray
    |   'true'
    |   'false'
    |   'null'
    ;

jsonArray
    :   '[' value+=jsonValue (',' value+=jsonValue)* ']'
    ->  ^(ARRAY $value+)
    ;

string
    :   String
    ->  ^(STRING String)
    ;
 
// If you want to conform to the RFC, use a validating semantic predicate to check the result.
// You can omit the check if you want. The parser will still recognize valid JSON and it will
// allow numbers with leading zeroes.
// See the second note above for an alternate approach using the tree parser.
// This could be more efficient (e.g. pre-compile the pattern), but I'm going for clarity here.
number
    :   n=Number {Pattern.matches("(0|(-?[1-9]\\d*))(\\.\\d+)?", n.getText())}? Exponent?
    ->  ^(NUMBER Number Exponent?)
    ;

Identifier
    :   ('a'..'z'|'A'..'Z'|'_'|'$') ('a'..'z'|'A'..'Z'|'_'|'0'..'9'|'$')*
    ;

// Simple, but more permissive than the RFC allows. See number above for a validity check.
Number  : '-'? Digit+ ( '.' Digit+)?;
 
Exponent: ('e'|'E') '-'? Digit+;
 
String  :
    '"' ( EscapeSequence | ~('\u0000'..'\u001f' | '\\' | '\"' ) )* '"'
    ;
 
fragment EscapeSequence
        :   '\\' (UnicodeEscape |'b'|'t'|'n'|'f'|'r'|'\"'|'\''|'\\')
        ;
 
fragment UnicodeEscape
    : 'u' HexDigit HexDigit HexDigit HexDigit
    ;
 
fragment HexDigit
    : '0'..'9' | 'A'..'F' | 'a'..'f'
    ;
 
fragment Digit
    : '0'..'9'
    ;

WS
    : (' ' |'\t' |'\r' |'\n' )+ { $channel=HIDDEN; }
    ;

COMMENT
    :   '/*' ( options {greedy=false;} : . )* '*/' { $channel=HIDDEN; }
    ;

LINE_COMMENT
    : '//' ~('\n'|'\r')* '\r'? '\n' { $channel=HIDDEN; }
    ;

