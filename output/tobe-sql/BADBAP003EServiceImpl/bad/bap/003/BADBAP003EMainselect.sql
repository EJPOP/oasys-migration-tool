SELECT D1.CPADT_DSPS_INST_CD /* 위탁대행감사처분기관코드 */,
    D2.PROF_RBPRSN_INST_NM /* 증명책임자기관명 */,
    D1.CPADT_CNSGN_YR /* 위탁대행감사위탁년도 */,
    D1.CPADT_SQNO /* 위탁대행감사순번 */,
    D1.CPADT_NTFCTN_YMD /* 위탁대행감사통보일자 */,
    D1.CPADT_NTFCTN_INST_CD /* 위탁대행감사통보기관코드 */,
    D1.CPADT_NTFCTN_PIC_ENO /* 위탁대행감사통보담당자직원전산번호 */,
    D1.CPADT_NTFCTN_DEPT_NM /* 위탁대행감사통보부서명 */,
    D1.CPADT_RCPT_STTS_SECD /* 위탁대행감사접수상태구분코드 */,
    D1.CPADT_RCPT_YMD /* 위탁대행감사접수일자 */,
    D1.CPADT_CLR_ENO /* 위탁대행감사접수자직원전산번호 */,
    D1.CPADT_CNSGN_SECD /* 위탁대행감사위탁구분코드 */,
    (SELECT AS CD_NM
FROM TBDCMACM013C WHERE CMM_CD_DVSN_CD = '1000207' AND CD = D1.CPADT_CNSGN_SECD AND NVL(USE_YN, 'N') = 'Y') AS DLG_CSM_DVSN_NM /* 위임위탁구분명 */ ,D1.ACADT_SECD /* 감사실시구분코드 */ ,D1.ADT_MTTR_NM /* 감사사항명 */ ,D1.CPADT_EMPHS_CN /* 감사중점사항 */ ,D1.CPADT_ENFRC_CN /* 감사실시범위내용 */ ,D1.ETC_MTTR_CN /* 기타사항 */ ,D1.PRCS_TERM_YMD /* 처리기한일 */ ,D1.CPADT_TRGT_SECD /* 감사대상업무코드 */ ,D1.CVLCPT_DOC_ID /* 문서ID */ ,D1.ADT_PLAN_DOC_TPCD /* 문서유형코드 */ ,D1.CPADT_DSPS_INLDR_JBPS_NM /* 기관장직위명 */ ,D1.CPADT_CNSGN_RQT_CN /* 대행요구위탁내용 */ ,D1.CPADT_RSLT_CN /* 감사결과접수내용 */ ,D1.CPADT_DCN /* 감사일수기간내용 */ ,D1.CPADT_RBPRSN_CN /* 책임자사항 */ ,D1.CPADT_PTOUT_NOCS_CN /* 지적건수내용 */ ,D1.FRST_USER_ID /* 최초사용자ID */ ,D1.LAST_USER_ID /* 최종사용자ID */ ,D1.LAST_PRCS_DEPT_CD /* 최종거래부서코드 */ ,D1.LAST_MENU_ID /* 최종메뉴ID */ ,D1.FRST_REG_DT /* 최초등록일시 */ ,D1.LAST_MDFCN_DT /* 최종수정일시 */ FROM TB_BADBAP001M D1 /* 위탁및대행감사기본 */ ,TBDCMACM015M D2 /* 기관정보기본 */ WHERE 1=1 AND D1.INST_CD = D2.INST_CD(+) AND D1.INST_CD = #INST_CD# /* 기관코드 */ AND D1.LINK_YR = #LINK_YR# /* 연도*/ AND D1.CPADT_SQNO = #CPADT_SQNO# /* 위임위탁번호 */