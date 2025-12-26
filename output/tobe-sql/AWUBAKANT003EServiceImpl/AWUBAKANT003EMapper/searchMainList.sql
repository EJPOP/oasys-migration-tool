/* AWU.BAK.BRR.AWUBAKANT003EMapper.searchMainList */ SELECT T1.CFR_DVSN_CD,
    T1.CFR_SRNO,
    T1.RSCH_ASMT_TTL /* 연구과제제목 */,
    TO_CHAR(TO_DATE(T1.DCS_YMD, 'YYYYMMDD'), 'YYYY-MM-DD') AS DCS_YMD /* 의결일자 */,
    T1.RLS_YN /* 공개여부 */,
    T1.FRST_USER_ID /* 최초사용자아이디 */,
    T1.LAST_USER_ID /* 최종사용자아이디 */,
    T1.LAST_PRCS_DEPT_CD /* 최종처리부서코드 */,
    T1.LAST_MENU_ID /* 최종메뉴아이디 */,
    TO_CHAR(T1.FRST_REG_DT, 'YYYY-MM-DD') AS FRST_REG_DT /* 최초등록일시 */,
    T1.LAST_MDFCN_DT /* 최종수정일시 */
FROM TBAWUBAKANT001M T1 WHERE T1.CFR_DVSN_CD = #{cfrDvsnCd} AND SUBSTR(T1.VT_DT,0,4) = #{srchVtDt} AND T1.RSCH_ASMT_TTL LIKE '%'||#{srchTitNm}||'%' ORDER BY T1.FRT_REGI_DH DESC