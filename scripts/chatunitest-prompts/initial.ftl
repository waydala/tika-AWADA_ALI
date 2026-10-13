The focal method is `${method_sig}` in the focal class `${class_name}`,
<#if method_invocation_codes_outerclass?has_content>
    Below are examples of existing code in the project where the target method is invoked.
    These snippets can serve as a reference for understanding how the method is used in contexts.
    <#list method_invocation_codes_outerclass as invoke_code>
        ```
        ${invoke_code}
        ```
    </#list>
<#elseif method_invocation_codes_innerclass?has_content>
    Below are examples of existing code in the focal class where the target method is invoked.
    These snippets can serve as a reference for understanding how the method is used in contexts,
    note that you should initialize the focal class object before invoking the method.
    <#list method_invocation_codes_innerclass as invoke_code>
        ```
        ${invoke_code}
        ```
    </#list>
</#if>
Information of the focal method is
```${full_fm}```.

<#if other_method_sigs?has_content>
    Signatures of Other methods in the focal class are `${other_method_sigs}`.
</#if>
<#list c_deps?keys as key>
    The brief information of dependent class `${key}` is
    ```${c_deps[key]}```.
</#list>
<#list m_deps?keys as key>
    The brief information of dependent class `${key}` is
    ```${m_deps[key]}```.
</#list>

For this focal class, these public methods are available:
LookaheadInputStream(InputStream stream, int n), read(), read(byte[] b, int off, int len),
available(), markSupported(), mark(int limit), reset(), close(), skip(long n).
Never access the PRIVATE fields position, buffered, buffer, stream or mark.
To check remaining buffered bytes use available(), not a field access.
For array reads, allocate a destination large enough for off + len.
Test only returned read counts and the destination byte array contents.
