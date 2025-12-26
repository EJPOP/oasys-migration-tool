SELECT T2.CVLCPT_DCLR_SECD /* 민원신고구분코드 */,
    T2.CVLCPT_RCPT_YR /* 민원접수년도 */,
    #RM_STTM_ACP_NO# AS CVLCPT_DCLR_RCPT_NO /* 민원신고접수번호 */,
    #DVSN_CD# AS USER_HSTRY_SECD /* 사용자이력구분코드 */,
    T2.CVLCPT_SQNO /* 민원순번 */,
    T2.SLCT_RPPS_NM /* 청탁피신고자명 */,
    T2.SLCT_RPPS_TELNO /* 청탁피신고자전화번호 */,
    T2.SLCT_RPPS_CR_NM /* 청탁피신고자직업명 */,
    T2.SLCT_RPPS_ADDR /* 청탁피신고자주소 */,
    T2.SLCT_CVLPR_CORP_NM /* 청탁민원인법인명 */
FROM TB_CSPDCP812M T2 WHERE T2.CVLCPT_DCLR_SECD = '1' AND T2.CVLCPT_RCPT_YR = #CVLCPT_RCPT_YR# AND T2.CVLCPT_DCLR_RCPT_NO = #CVLCPT_DCLR_RCPT_NO#