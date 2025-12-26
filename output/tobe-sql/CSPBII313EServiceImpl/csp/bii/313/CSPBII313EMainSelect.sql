SELECT INFO_RLPR_NM /* 정보관계자명 */,
    CHC_NAM /* 한자성명 */,
    CRU_RRNO_SCRTY /* 부패주민등록번호보안 */,
    OGDP_INST_CD /* 소속기관코드 */,
    (SELECT B.PROF_RBPRSN_INST_NM AS PROF_RBPRSN_INST_NM /* 증명책임자기관명 */
FROM TBDCMACM015M B WHERE B.INST_CD = A.OGDP_INST_CD) AS TPOF_OGDP_INST_NM /* 소속기관명 */ , CLSS_CD /* 계급코드 */ , DTY_RK_CD /* 직무등급코드 */ , OCPT_SECD /* 직종코드 */ , JBGD_NM /* 직급명 */ , CRSP_OD_RANK_CD /* 상응구직급코드 */ , CRU_OGDP_DEPT_NM /* 소속부서명 */ , JBPS_NM /* 직위명 */ , OTSD_EXPRT_ADDR /* 주소 */ , CRU_TELNO /* 전화번호 */ , CNSTN_MBCMT_FAX_NO /* 팩스번호 */ , EML_ADDR /* 이메일주소 */ , DICE_DVSN_CD /* 제대구분코드 */ , EMP_RSN_CD /* 면제사유코드 */ , BEF_WRK_ORG_CD /* 전근무기관코드 */ , RLG_CD /* 종교코드 */ , EPS_MNG_DVSN_CD /* 중점관리구분코드 */ , RSG_YN /* 퇴직여부 */ , RTRM_YMD /* 퇴직일 */ , MTRL_SRC_CD /* 자료출처코드 */ , ORGCHF_YN /* 기관장여부 */ , EVL_TG_DVSN_CD /* 평가대상구분코드 */ , EVL_PST_DVSN_CD /* 평가직위구분코드 */ , ETC_TXT /* 기타내용 */ , CVLCPT_DOC_ID /* 사진문서ID */ FROM TBCSPBII300M A /* 대인정보기본 */ WHERE A.CRU_RRNO_SCRTY = #CRU_RRNO_SCRTY#