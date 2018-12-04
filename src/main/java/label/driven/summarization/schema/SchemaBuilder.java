package label.driven.summarization.schema;

import com.cloudera.org.codehaus.jackson.map.ObjectMapper;
import label.driven.summarization.schema.json.SchemaFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @author LIRIS
 * @version 1.0
 * @since 1.0 5/16/18.
 */
public class SchemaBuilder {
    private String path;

    public SchemaBuilder fromSchemaFile( String path) throws FileNotFoundException {

        File file = new File(path);
        if (!file.exists())
            throw new FileNotFoundException(String.format("File '%s' not found.", path));

        this.path = path;
        return this;
    }

    public Schema build() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return new GraphSchema(mapper.readValue(new File(path), SchemaFile.class));
    }

    public String getPath() {
        return path;
    }
}
