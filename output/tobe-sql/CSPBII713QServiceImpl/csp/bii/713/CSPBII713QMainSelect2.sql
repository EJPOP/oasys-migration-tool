SELECT /* 감사정보 상세조회 영역 */ A.INFO_YR AS INFO_YR /* 정보년도 */,
    A.FRS_MDM_SQNO /* 포렌식매체순번 */,
    A.RLS_YN /* 공개여부 */,
    A.ETC_LBSVC_MNG_DEPT_CD /* 기타용역관리부서코드 */
FROM TB_CSPBII170M A /* 감사정보기본 */ , TB_CSPBII171L B /* 감사정보내역 */ , (SELECT D.ATRZ_DOC_NO /* 결재문서번호 */,
    DECODE(D.TPOF_ATRZ_SECD, 'AudInfoSend', F_CODE_NM('1000176', E.JBPS_CD), NULL) AS TPOF_ATRZ_SECD /* 제보결재구분코드 */,
    DECODE(D.TPOF_ATRZ_SECD, 'AudInfoSend', F_CODE_NM('1000173', E.JBGD_SECD), NULL) AS TPOF_ATRZ_SECD /* 제보결재구분코드 */,
    DECODE(D.TPOF_ATRZ_SECD, 'AudInfoSend', E.USER_NM, NULL) AS TPOF_ATRZ_SECD /* 제보결재구분코드 */,
    DECODE(D.TPOF_ATRZ_SECD, 'AudInfoHndl2', F_CODE_NM('1000176', E.JBPS_CD), NULL) AS TPOF_ATRZ_SECD /* 제보결재구분코드 */,
    DECODE(D.TPOF_ATRZ_SECD, 'AudInfoHndl2', F_CODE_NM('1000173', E.JBGD_SECD), NULL) AS TPOF_ATRZ_SECD /* 제보결재구분코드 */,
    DECODE(D.TPOF_ATRZ_SECD, 'AudInfoHndl2', E.USER_NM, NULL) AS TPOF_ATRZ_SECD /* 제보결재구분코드 */,
    TO_CHAR(D.SBM_DH, 'YYYY.MM.DD') AS RTSO_YMD /* 상신일자 */
FROM TBDCMACM022L D /* 결재문서내역 */ , TBDCMACM001M E /* 사용자정보기본 */ WHERE D.FNL_APV_CNB = E.TRGT_NOP_ENO AND D.ATRZ_STTS_SECD = '3' /* [3:결재승인]*/ AND D.TPOF_ATRZ_SECD IN ('AudInfoSend', 'AudInfoHndl2') /* [AudInfoSend:감사정보제출, AudInfoHndl:감사정보처리] */ ) D WHERE A.INFO_YR = B.INFO_YR AND A.FRS_MDM_SQNO = B.INFO_SQNO AND A.ATRZ_DOC_NO = D.ATRZ_DOC_NO(+) AND A.INFO_YR = #INFO_YR# AND A.FRS_MDM_SQNO = #FRS_MDM_SQNO# AND (A.INFO_YR, A.FRS_MDM_SQNO) IN (WITH VW AS (SELECT REGEXP_SUBSTR(#strYeList#, '[^||]+', 1, LEVEL) AS INFO_YR /* 정보년도 */,
    REGEXP_SUBSTR(#strSrnoList#, '[^||]+', 1, LEVEL) AS FRS_MDM_SQNO /* 포렌식매체순번 */
FROM DUAL CONNECT BY LEVEL &lt;= REGEXP_COUNT(#strYeList#, '[^|]+')) SELECT INFO_YR /* 정보년도 */,
    FRS_MDM_SQNO /* 포렌식매체순번 */
FROM VW ) AND A.ATRZ_DOC_NO = #ATRZ_DOC_NO# AND A.NTFCTN_RCPT_YMD BETWEEN #strStrAcpDt# AND #strEndAcpDt# AND A.CRU_RCPT_DT BETWEEN #strStrAcpDh# AND #strEndAcpDh# AND A.HNDT_YMD BETWEEN #strStrAcpDt# AND #strEndAcpDt# AND A.OVRL_PIC_ENO = #OVRL_PIC_ENO# ORDER BY A.INFO_YR , A.FRS_MDM_SQNO