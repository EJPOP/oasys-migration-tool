SELECT D1.PIC_ENO AS PIC_ENO /* 담당자직원전산번호 */,
    D1.PIC_NM AS PIC_NM /* 담당자명 */,
    D1.TKCG_DEPT_NM AS TKCG_DEPT_NM /* 담당부서명 */,
    D1.PIC_ID AS PIC_ID /* 담당자아이디 */,
    D1.CHR_DPT_CD AS CHR_DPT_CD /* 담당부서코드 */,
    D1.CHGR_NO AS CHGR_NO /* 담당자ID */
FROM TBCSPFPL004M D1 where 1=1 D1.PIC_NM LIKE '%'||#strChgrNm# ||'%' D1.TKCG_DEPT_NM LIKE '%'||#strDeptNm# ||'%' D1.PIC_ENO LIKE '%'||#StrChgrCnb# ||'%' ORDER BY D1.PIC_NM ASC