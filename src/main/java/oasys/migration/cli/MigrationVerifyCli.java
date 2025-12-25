package oasys.migration.cli;

import oasys.migration.callchain.ServiceSqlCall;
import oasys.migration.callchain.ServiceSqlXrefLoader;
import oasys.migration.sql.SqlStatement;
import oasys.migration.sql.SqlStatementRegistry;

import java.util.List;

public class MigrationVerifyCli {

    public static void main(String[] args) {

        ServiceSqlXrefLoader xrefLoader = new ServiceSqlXrefLoader();
        SqlStatementRegistry registry = new SqlStatementRegistry();

        // 🔥 sqls 전체 인덱싱
        registry.initialize();

        List<ServiceSqlCall> calls =
                xrefLoader.load("mapping/service_sql_xref.csv");

        System.out.println("Loaded CallChain = " + calls.size());

        calls.stream()
                .limit(30)
                .forEach(call -> {

                    SqlStatement stmt =
                            registry.get(
                                    call.getMapperNamespace(),
                                    call.getSqlId()
                            );

                    if (stmt == null) {
                        System.out.println("[MISS] "
                                + call.getMapperNamespace()
                                + "." + call.getSqlId());
                        return;
                    }

                    System.out.println("--------------------------------------------------");
                    System.out.println("Service : "
                            + call.getServiceClass()
                            + "." + call.getServiceMethod());
                    System.out.println("Mapper  : "
                            + call.getMapperNamespace()
                            + "." + call.getSqlId());
                    System.out.println("XML     : " + stmt.getMapperFile());
                    System.out.println("Type    : " + stmt.getSqlType());
                    System.out.println("SQL     : " + stmt.getSqlText());
                });
    }
}
