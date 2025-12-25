package oasys.migration.alias;

public class ColumnMapping {

    public final String domain;           // 주제영역

    public final String asisTableId;
    public final String asisTableName;
    public final String asisColumnId;
    public final String asisColumnName;

    public final String tobeTableId;
    public final String tobeTableName;
    public final String tobeColumnId;
    public final String tobeColumnName;

    public ColumnMapping(
            String domain,
            String asisTableId,
            String asisTableName,
            String asisColumnId,
            String asisColumnName,
            String tobeTableId,
            String tobeTableName,
            String tobeColumnId,
            String tobeColumnName
    ) {
        this.domain = domain;
        this.asisTableId = asisTableId;
        this.asisTableName = asisTableName;
        this.asisColumnId = asisColumnId;
        this.asisColumnName = asisColumnName;
        this.tobeTableId = tobeTableId;
        this.tobeTableName = tobeTableName;
        this.tobeColumnId = tobeColumnId;
        this.tobeColumnName = tobeColumnName;
    }
}
