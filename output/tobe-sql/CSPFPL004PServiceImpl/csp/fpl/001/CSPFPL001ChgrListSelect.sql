select A1.PIC_NM /* 담당자명 */,
    A1.PIC_ENO /* 담당자직원전산번호 */,
    A1.CHR_DPT_CD,
    A1.TKCG_DEPT_NM /* 담당부서명 */,
    A1.PIC_ID /* 담당자아이디 */,
    A1.DIV
FROM ( select D1.PIC_NM AS PIC_NM /* 담당자명 */,
    D1.PIC_ENO AS PIC_ENO /* 담당자직원전산번호 */,
    D1.CHR_DPT_CD AS CHR_DPT_CD,
    D1.TKCG_DEPT_NM AS TKCG_DEPT_NM /* 담당부서명 */,
    D1.PIC_ID AS PIC_ID /* 담당자아이디 */,
    '기획담당자' AS DIV
from tbcspfpl004m D1 UNION select D2.USER_NM AS USER_NM /* 사용자명 */,
    D2.TRGT_NOP_ENO AS TRGT_NOP_ENO /* 대상인원직원전산번호 */,
    D2.SRECS_DEPT_CD /* 심사재심의부서코드 */,
    F_DPT_INFO(D2.SRECS_DEPT_CD, '1') AS TKCG_DEPT_NM /* 담당부서명 */,
    D2.USER_ID AS USER_ID /* 사용자아이디 */,
    '일반사용자' AS DIV
from tbdcmacm001m D2) A1 where 1=1 A1.PIC_NM LIKE '%'||#strChgrNm#||'%' A1.TKCG_DEPT_NM LIKE '%'||#strChgrDptNm#||'%' A1.PIC_ENO LIKE '%'||#strChgrCnb#||'%' A1.DIV = #strDiv# ORDER BY A1.DIV