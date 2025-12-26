SELECT B.LEND_BOKG_APL_DT /* 대출예약신청일 */,
    A.BOO_NO /* 도서번호 */,
    A.BOO_NO||'-'||A.BOO_VLE_NO AS BOO_NO,
    A.BOO_VLE_NO,
    A.CAT_SYM_NM /* 분류기호명 */,
    SUBSTR(A.CAT_SYM_NM,1,1)||'-'||SUBSTR(A.CAT_SYM_NM,2,3)||'-'||SUBSTR(A.CAT_SYM_NM,5,3) AS SUBSTR,
    A.BOO_TIT_NM /* 도서제목명 */,
    A.RSCH_RPTP_AUT_NM /* 연구보고서저자명 */,
    B.BOO_BOKG_NO,
    NVL(A.BOO_LEND_DVSN_CD,'1') AS BOO_LEND_DVSN_CD /* 입고여부 */,
    A.COPT_CAT_CD /* 소관분류코드 */,
    A.BOO_KND_CD /* 도서종류코드 */,
    B.BOO_BOKG_SLN /* 도서예약연번 */
FROM TBCSPQBO001M A /*도서자료기본*/ ,( SELECT B.*,
    RANK() OVER (PARTITION BY B.COPT_CAT_CD, B.BOO_KND_CD, B.BOO_NO, B.CAT_SYM_NM, B.BOO_VLE_NO ORDER BY B.BOO_BOKG_SLN ASC) AS RANK
FROM TBCSPQBO003H B /*도서대출예약이력*/ WHERE B.LEND_BOKG_STA_CD = 'Y' ) B WHERE 1=1 AND A.COPT_CAT_CD = B.COPT_CAT_CD AND A.BOO_KND_CD = B.BOO_KND_CD AND A.BOO_NO = B.BOO_NO AND A.CAT_SYM_NM = B.CAT_SYM_NM AND A.BOO_VLE_NO = B.BOO_VLE_NO AND A.COPT_CAT_CD = #strCOPT_CAT_CD# AND B.LEND_BOKG_PEN_CNB = #strCNB# AND B.LEND_BOKG_APL_DT &gt;= #strLEND_BOKG_APL_DTS# AND B.LEND_BOKG_APL_DT &lt;= #strLEND_BOKG_APL_DTE# AND ( A.BOO_LEND_DVSN_CD = #strBOO_LEND_DVSN_CD# OR A.BOO_LEND_DVSN_CD IS NULL ) AND ( A.BOO_LEND_DVSN_CD = #strBOO_LEND_DVSN_CD# ) AND A.BOO_TIT_NM like '%'||#strBOO_TIT_NM1#||'%' AND (A.BOO_TIT_NM like '%'||#strBOO_TIT_NM1#||'%' AND A.BOO_TIT_NM like '%'||#strBOO_TIT_NM3#||'%') AND (A.BOO_TIT_NM like '%'||#strBOO_TIT_NM1#||'%' OR A.BOO_TIT_NM like '%'||#strBOO_TIT_NM3#||'%') AND (A.BOO_TIT_NM like '%'||#strBOO_TIT_NM1#||'%' AND A.BOO_TIT_NM like '%'||#strBOO_TIT_NM2#||'%') AND (A.BOO_TIT_NM like '%'||#strBOO_TIT_NM1#||'%' OR A.BOO_TIT_NM like '%'||#strBOO_TIT_NM2#||'%') AND (A.BOO_TIT_NM like '%'||#strBOO_TIT_NM1#||'%' AND A.BOO_TIT_NM like '%'||#strBOO_TIT_NM2#||'%' AND A.BOO_TIT_NM like '%'||#strBOO_TIT_NM3#||'%') AND (A.BOO_TIT_NM like '%'||#strBOO_TIT_NM1#||'%' AND (A.BOO_TIT_NM like '%'||#strBOO_TIT_NM2#||'%' OR A.BOO_TIT_NM like '%'||#strBOO_TIT_NM3#||'%')) AND ( (BOO_TIT_NM like '%'||#strBOO_TIT_NM1#||'%' OR BOO_TIT_NM like '%'||#strBOO_TIT_NM2#||'%') AND BOO_TIT_NM like '%'||#strBOO_TIT_NM3#||'%') AND (A.BOO_TIT_NM like '%'||#strBOO_TIT_NM1#||'%' OR A.BOO_TIT_NM like '%'||#strBOO_TIT_NM2#||'%' OR A.BOO_TIT_NM like '%'||#strBOO_TIT_NM3#||'%') ORDER BY B.LEND_BOKG_APL_DT DESC