SELECT min(INQ_YMD) AS INQ_YMD /* 조회일자 */,
    SRCH_DPT_NM,
    INQPR_NM /* 조회자명 */,
    ADT_BKMK_YMD /* 감사즐겨찾기일자 */,
    ADT_BKMK_MTTR_NM /* 감사즐겨찾기사항명 */,
    ADT_BKMK_RGTR_NM /* 감사즐겨찾기등록자명 */,
    ADT_BKMK_RMV_YMD /* 감사즐겨찾기해제일자 */,
    INFO_YR /* 정보년도 */,
    FRS_MDM_SQNO /* 포렌식매체순번 */,
    INFO_LOG_SQNO /* 정보로그순번 */
FROM ( SELECT DISTINCT TO_CHAR(TO_DATE(A.INQ_YMD), 'YYYY.MM.DD') AS INQ_YMD /* 조회일자 */,
    (SELECT B.CNSTN_MBCMT_DEPT_NM AS CNSTN_MBCMT_DEPT_NM /* 자문위원부서명 */
FROM TBDCMACM002M B WHERE B.SRECS_DEPT_CD = A.SRCH_DEPT_CD) AS SRCH_DPT_NM /* 조회부서명 */ , A.INQPR_NM /* 조회자성명 */ , '' AS ADT_BKMK_YMD /* 감사찜일 */ , '' AS ADT_BKMK_MTTR_NM /* 감사찜사항명 */ , '' AS ADT_BKMK_RGTR_NM /* 감사찜관련자명 */ , '' AS ADT_BKMK_RMV_YMD /* 감사찜해제일 */ , '' AS ADT_BKMK_MDFR_NM /* 감사찜해제자성명 */ , '' AS INFO_YR /* 연도 */ , '' AS FRS_MDM_SQNO /* 일련번호 */ , '' AS INFO_LOG_SQNO /* 관련자순번 */ FROM TBCSPBII150L A /* 감사정보조회로그감사찜내역 */ ,TBCSPBII170M B WHERE 1=1 AND B.INFO_YR = A.INFO_YR AND B.FRS_MDM_SQNO = A.FRS_MDM_SQNO AND A.INFO_YR = #INFO_YR# AND A.FRS_MDM_SQNO = #FRS_MDM_SQNO# ) GROUP BY SRCH_DPT_NM, INQPR_NM, ADT_BKMK_YMD, ADT_BKMK_MTTR_NM, ADT_BKMK_RGTR_NM, ADT_BKMK_RMV_YMD, INFO_YR, FRS_MDM_SQNO, INFO_LOG_SQNO ORDER BY INQ_YMD ASC