SELECT FNNC_INFO_SBMSN_RQT_PBLCN_NO /* 금융정보제출요구발부번호 */,
    ADTR_NM /* 감사인명 */,
    PBLCN_YMD /* 발부일자 */,
    BRNCH_NM /* 지점명 */,
    CRU_RRNO_SCRTY /* 부패주민등록번호보안 */,
    ACTNO_SCRTY /* 계좌번호보안 */,
    CO_NM /* 회사명 */,
    INFO_RLPR_NM /* 정보관계자명 */,
    DECODE(USE_PRPS_SECD,'1','회계','2','금융') AS USE_PRPS_SECD /* 사용목적구분코드 */,
    RQT_DLNG_INFO_DSCTN /* 요구거래정보내역 */,
    NTFCTN_RCPT_YMD /* 통보접수일자 */,
    PUBOL_SRCH_NOCS /* 공직자검색건수 */,
    GNRL_INDV_SRCH_NOCS /* 일반개인검색건수 */,
    LINK_YR /* 연계년도 */,
    SRECS_DEPT_CD /* 심사재심의부서코드 */,
    SPCL_MNG_IMPL_SQNO /* 특수관리집행순번 */
FROM TB_BADJFS002D WHERE 1=1 AND SRECS_DEPT_CD IN (SELECT SRECS_DEPT_CD /* 심사재심의부서코드 */
FROM TABLE(F_AUTH_DEPT(#strCnb#, #strDptAuth#, '', #strDptCd#))) AND LINK_YR = #LINK_YR# AND SRECS_DEPT_CD = #SRECS_DEPT_CD# AND SPCL_MNG_IMPL_SQNO = #SPCL_MNG_IMPL_SQNO# ORDER BY SPCL_MNG_IMPL_SQNO DESC ,FNNC_INFO_SBMSN_RQT_PBLCN_NO