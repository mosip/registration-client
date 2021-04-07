package registrationtest.pojo.schema;

import java.util.Date;
import java.util.List;
// import com.fasterxml.jackson.databind.ObjectMapper; // version 2.11.1
// import com.fasterxml.jackson.annotation.JsonProperty; // version 2.11.1
/* ObjectMapper om = new ObjectMapper();
Root root = om.readValue(myJsonString), Root.class); */

public class Root {


    public String id;
    public double idVersion;
    public List<Schema> schema;
    public String schemaJson;
    public Date effectiveFrom;

  Root(){}
    Root(    String id,
             double idVersion,
             List<Schema> schema,
             String schemaJson,
             Date effectiveFrom)
    {
        this.id=id;
        this.idVersion=idVersion;
        this.schema=schema;
        this.schemaJson=schemaJson;
        this.effectiveFrom=effectiveFrom;
    }



    public String getId() {
        return id;
    }
    public double getIdVersion() {
        return idVersion;
    }

    public List<Schema> getSchema() {
        return schema;
    }

    public String getSchemaJson() {
        return schemaJson;
    }

    public Date getEffectiveFrom() {
        return effectiveFrom;
    }
}
