# MybatisPlugin

### There are two intercepters in this repo,one is used to add createdtime/createdby or udpatedtime/updatedby when insert or update a record, another is used as datasecurity plugin when try to fetch db result

## How to use it ? 

### Suppose mybatis config file is `mybatis-config.xml`, add these two plugins in the plugin node 

```properties
<plugins>
  <plugin interceptor="path/to/intercepter"/>
</plugins>
```
