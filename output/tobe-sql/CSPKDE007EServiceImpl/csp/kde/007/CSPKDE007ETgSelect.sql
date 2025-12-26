SELECT D1.JUD_YR AS JUD_YR /* 판정년도 */,
    D1.JUD_NO AS JUD_NO /* 판정번호 */,
    D1.FRS_MDM_SQNO AS FRS_MDM_SQNO /* 포렌식매체순번 */,
    D1.INFO_RLPR_NM AS INFO_RLPR_NM /* 정보관계자명 */,
    D1.SLCT_OGDP_NM AS SLCT_OGDP_NM /* 청탁소속명 */,
    D1.JBGD_SECD AS JBGD_SECD /* 직급구분코드 */,
    F_CODE_NM('1000173',D1.JBGD_SECD) AS JBGD_NM /* 직급명 */,
    D1.JBPS_CD AS JBPS_CD /* 직위코드 */,
    F_CODE_NM('1000176',D1.JBPS_CD) AS JBPS_NM /* 직위명 */,
    D1.ACNT_RANK_NM AS ACNT_RANK_NM /* 회계직명 */
FROM TBCSPKDE004L D1 /* 변상판정대상자내역 */ WHERE 1=1 /* 판정년도 */ AND D1.JUD_YR = #strJudYr# /* 판정번호 */ AND D1.JUD_NO = #strJudNo# ORDER BY D1.FRS_MDM_SQNO