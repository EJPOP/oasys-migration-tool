SELECT '' AS REG_SECD /* 등록구분코드 */,
    B.LINK_YR /* 연계년도 */,
    B.CRS_CD /* 과정코드 */,
    B.CRS_CDN /* 과정기수 */,
    B.SUBJ_SNO /* 과목순번 */,
    B.SUBJ_CD /* 과목코드 */,
    A.BLL_SBJCT_NM /* 계산서과목명 */,
    B.LCT_TM_HM /* 강의시간 */,
    B.JON_TM_HM /* 참여시간 */,
    B.ETC_TM_HM /* 기타시간 */,
    B.PFSR_ID /* 강사ID */,
    B.SUBJ_YN /* 과목여부 */
FROM TBCSPSAE101M A , TBCSPSAE101D B WHERE A.SUBJ_CD = B.SUBJ_CD AND A.USE_YN = 'Y' AND B.SUBJ_YN = 'Y' AND B.LINK_YR = #LINK_YR# AND B.CRS_CD = #strEDU_CRS_CD# AND B.CRS_CDN = #CRS_CDN# ORDER BY TO_NUMBER(B.SUBJ_SNO)