package oasys.migration.alias;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

public class AsisSelectParser {

    public PlainSelect parse(String sql) {
        try {
            Select select = (Select) CCJSqlParserUtil.parse(sql);
            return (PlainSelect) select.getSelectBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse SELECT SQL", e);
        }
    }
}
