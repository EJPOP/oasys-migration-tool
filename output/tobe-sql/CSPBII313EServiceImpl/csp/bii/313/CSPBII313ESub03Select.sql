SELECT A.CRU_RRNO_SCRTY /* 부패주민등록번호보안 */,
    A.FRS_MDM_SQNO /* 포렌식매체순번 */,
    A.ENT_DT /* 입학일 */,
    A.GRD_DT /* 졸업일 */,
    A.DEG_CD /* 학위코드 */,
    A.SCHL_NM /* 학교명 */,
    A.MJR_TXT /* 전공학과내용 */
FROM TBCSPBII320L A WHERE A.CRU_RRNO_SCRTY = #CRU_RRNO_SCRTY# ORDER BY A.FRS_MDM_SQNO DESC