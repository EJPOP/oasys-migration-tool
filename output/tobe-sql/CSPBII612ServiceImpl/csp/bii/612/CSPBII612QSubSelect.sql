SELECT A.INFO_RLPR_NM /* 정보관계자명 */,
    A.MTRL_DVSN_CD /* 자료구분 */,
    A.EPS_MNG_DVSN_CD /* 중점관리구분 */,
    A.RSG_YN /* 퇴직여부 */,
    A.WRK_RANK_CD /* 근무직급코드 */,
    A.OGDP_INST_CD /* 소속기관코드 */,
    (SELECT B.PROF_RBPRSN_INST_NM AS PROF_RBPRSN_INST_NM /* 증명책임자기관명 */
FROM TBDCMACM015M B WHERE B.INST_CD = A.OGDP_INST_CD) AS TPOF_OGDP_INST_NM /* 소속기관명 */ , A.INST_SECD /* 기관구분코드 */ , A.DTIL_ORG_NM /* 세부기관명 */ , A.DTIL_RANK_NM /* 세부직급명 */ , A.JOCP_DT /* 입사일 */ , A.RTRM_YMD /* 퇴사일 */ , A.CRU_RRNO_SCRTY /* 암호화주민등록번호 */ , A.RRN_SRNO /* 주민등록번호일련번호 */ , (SELECT TO_CHAR(B.FRST_REG_DT, 'YYYY.MM.DD') || ' 등록' AS FRST_REG_DT /* 최초등록일시 */
FROM TBCSPBII300M B WHERE B.CRU_RRNO_SCRTY = A.CRU_RRNO_SCRTY) AS INSP_REGI_DT /* 대인감찰등록일 */ , A.CRU_RRNO_SCRTY AS CHG_BF_EIN /* 변경이전암호화주민등록번호 */ , A.RRN_SRNO AS CHG_BF_RRN_SRNO /* 변경이전주민등록번호일련번호 */ FROM TBCSPBII600M A /* 공무원_공공직원 기본 */ WHERE A.CRU_RRNO_SCRTY = #CRU_RRNO_SCRTY# AND A.RRN_SRNO = #strRrnSrno#