<?xml version="1.0" encoding="UTF-8"?>
<schema xmlns:sch="http://purl.oclc.org/dsdl/schematron" queryBinding="xslt2"
    xmlns:sqf="http://www.schematron-quickfix.com/validator/process"
    xmlns="http://purl.oclc.org/dsdl/schematron">
    <ns uri="http://example.com/dfdl/helloworld/" prefix="tns"/>
    <pattern name="Check structure">
        <_rule context="tns:helloWorld">
            <assert test="count(*) = 2 and count(word) = 2">The element World should have 2 child elements Named "word".</assert>
            <assert test="word[1] = 'Hello' and word[2] = 'world!'"> Verify word[1] and word[2] are correct.</assert>
        </rule>
    </pattern>
</schema>
