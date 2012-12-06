tree grammar DmlTree;

options {
	tokenVocab=Dml;
    ASTLabelType=CommonTree;
	backtrack=true;
	memoize=true;
}

@header {
package pt.ist.fenixframework.dml;

import java.net.URL;
}

compilationUnit[DomainModel model, URL sourceFile]
	@init {
        String packageName = "";
    }
    :   (p=definitions[model, packageName, sourceFile] { packageName = p; })*
    ;

definitions[DomainModel model, String packageName, URL sourceFile] returns [String newPackageName = packageName]
    :   p=packageDeclaration { newPackageName = p; }
    |   enumType[model]
    |   valueType[model]
    |   externalDeclaration[model, sourceFile]
    |   classDeclaration[model, packageName, sourceFile]
    |   relation[model, packageName, sourceFile]
    ;

packageDeclaration returns [String packageName]
    :   ^('package' name=qualifiedName)
        { packageName = name; }
    ;

enumType[DomainModel model]
    :   ^('enum' full=qualifiedName alias=qualifiedName?)
        { model.newEnumType(alias, full); }
    ;

valueType[DomainModel model]
    :   ^('valueType' typeName=type[model, true] name=qualifiedName? valueTypeBody?)
        { model.newValueType(name, typeName); }
    ;

valueTypeBody
    :   externalizationClause (internalizationClause)?
    ;

externalizationClause
    :   ^('externalizeWith' qualifiedName Identifier)
    ;

internalizationClause
    :   ^('internalizeWith' Identifier)
    ;

externalDeclaration[DomainModel model, URL sourceFile]
    :   ^('external' full=Identifier alias=Identifier?)
        { model.addExternalEntity(sourceFile, full.getText(), alias.getText()); }
    ;

classDeclaration[DomainModel model, String packageName, URL sourceFile]
    @init {
        DomainClass clazz = null;
    }
    :   ^('class' name=entityTypeIdentifier[packageName]
                  (^('extends' sc=entityTypeIdentifier[packageName]))?
                  (^('implements' ifs=entityTypeIdentifier[packageName]+))?
        {
        	DomainEntity superclass = null;
        	if (sc != null) {
                superclass = model.findClassOrExternal(sc);
                if (superclass == null) {
					throw new RecognitionException(input);
                }
        	}
        	clazz = new DomainClass(sourceFile, name, superclass, ifs);
        	model.addClass(clazz);
        }
        slotDeclaration[model, packageName, clazz]*)
    ;

slotDeclaration[DomainModel model, String packageName, DomainClass clazz]
    :   ^(SLOT slotType=type[model, false] name=Identifier meta=metadata?)
        { clazz.addSlot(new Slot(name.getText(), slotType, meta)); }
    ;

relation[DomainModel model, String packageName, URL sourceFile]
    @init {
        DomainRelation r = null;
    }
    :   ^('relation' name=Identifier
        {
            r = new DomainRelation(sourceFile, name.getText(), null, null);
            model.addRelation(r);
        }
        role[model, packageName, r]*)
    ;

role[DomainModel model, String packageName, DomainRelation rel]
    @init {
        Role r = null;
    }
    :   ^(ROLE roleType=entityTypeIdentifier[packageName] name=Identifier?
        {
            DomainEntity type = model.findClassOrExternal(roleType);
            if (type == null) {
                throw new RecognitionException(input);
            }
            r = new Role(name != null ? name.getText() : null, type);
            rel.addRole(r);
        }
        multiplicity[r]? indexed[r]? meta=metadata?)
        {
        	r.setMetadata(meta);
        }
    ;

multiplicity[Role r]
    :   ^(MULTIPLICITY range[r])
    ;

indexed[Role r]
    :   ^(INDEXED id=Identifier)
    	{ r.setIndexProperty(id.getText()); }
    ;

range[Role r]
    :   l=rangeBound '..' u=rangeBound
        {
            r.setMultiplicity(l, u);
        }
    |   u=rangeBound
        {
            r.setMultiplicity(0, u);
        }
    ;

rangeBound returns [int b]
    :   d=(Digit+) { b = Integer.parseInt(d.getText()); }
    |   '*' { b = Role.MULTIPLICITY_MANY; }
    ;

entityTypeIdentifier[String packageName] returns [String name = ""]
    :   t=qualifiedName
	    {
	        if ((packageName == null) || packageName.equals("")) {
	            name = t;
	        } else {
	            name = packageName + "." + t;
	        }
	    }
    |   '.' t=qualifiedName
	    {
	    	name = t;
	    }
    ;

type[DomainModel model, boolean create] returns [ValueType valueType]
	:	name=qualifiedName
	    {
	        if (create) {
	        	valueType = new PlainValueType(name);
	        } else {
	        	valueType = model.findValueType(name);
	        	if (valueType == null) {
	        	    throw new RecognitionException(input);
	        	}
	        }
	    }
	    (args=typeArguments[model, false] { valueType = new ParamValueType(valueType.getBaseType(), "<" + args + ">"); })?
	    array=(('[' ']')*) { if (array != null) valueType = new ArrayValueType(valueType, $array.text); }
	;

typeArguments[DomainModel model, boolean create] returns [String args]
    @init {
    	ArrayList<String> as = new ArrayList<String>();
    }
    :   '<' a=typeArgument[model, create] { as.add(a); } (',' a=typeArgument[model, create] { as.add(a); })* '>'
        { args = "<" + org.apache.commons.lang.StringUtils.join(as, ", ") + ">"; }
    ;

typeArgument[DomainModel model, boolean create] returns [String arg]
    :   t=type[model, create]
        { arg = t.getFullname(); }
    |   '?' (ext=('extends' | 'super') t=type[model, create])?
        { arg = "? " + $ext.text + " " + t.getFullname(); }
    ;

qualifiedName returns [String name]
    :   f=Identifier ('.' r=qualifiedName)?
	    { name = f.getText() + (r != null ? ("." + r) : ""); }
    ;

/* Json metadata */

metadata returns [org.json.simple.JSONObject object]
	@init {
		object = new org.json.simple.JSONObject();
	}
    :  ^(METADATA jsonPair[object]+)
    ;

jsonPair[org.json.simple.JSONObject object]
    :   ^(FIELD id=Identifier v=jsonValue)
        { object.put(id.getText(), v); }
    ;

jsonValue returns [Object value]
    :   S=string { value = S; }
    |   n=number { value = n; }
    |   m=metadata { value = m; }
    |   a=jsonArray { value = a; }
    |   'true' { value = true; }
    |   'false' { value = false; }
    |   'null' { value = null; }
    ;

jsonArray returns [org.json.simple.JSONArray array]
	@init {
		array = new org.json.simple.JSONArray();
	}
    :   ^(ARRAY l+=(jsonValue)+)
        { array.addAll(l.getChildren()); }
    ;

string returns [Object value]
    :   ^(STRING s=String)
        { value = s.getText(); }
    ;

number returns [Object value]
    :   ^(NUMBER n=Number e=Exponent?)
        { value = n.getText() + "e" + e.getText(); }
    ;
