package oasys.migration.alias;

public class AliasSqlResult {

    public final String result; // SUCCESS / SKIP
    public final String serviceClass;
    public final String namespace;
    public final String sqlId;
    public final String reason;

    public AliasSqlResult(
            String result,
            String serviceClass,
            String namespace,
            String sqlId,
            String reason
    ) {
        this.result = result;
        this.serviceClass = serviceClass;
        this.namespace = namespace;
        this.sqlId = sqlId;
        this.reason = reason;
    }
}
