SELECT A.INFO_YR /* 정보년도 */,
    A.FRS_MDM_SQNO /* 포렌식매체순번 */,
    A.INFO_RLPR_SQNO /* 정보관계자순번 */,
    A.CHG_RRN /* 변경주민등록번호 */
FROM TBCSPBII410L A /* 감사정보관련자임시내역 */ WHERE A.CHG_RRN IS NOT NULL SELECT A.CVLCPT_ADT_YR /* 민원감사년도 */,
    A.ADT_NO /* 감사번호 */,
    A.DSPS_RQT_SQNO /* 처분요구순번 */,
    A.SRECS_REL_INST_CD /* 심사재심의관계기관코드 */,
    A.RLPR_SQNO /* 관계자순번 */,
    A.CHG_RRN /* 변경주민등록번호 */
FROM TBCSPBII420L A /* 관계자기본임시내역 */ WHERE A.CHG_RRN IS NOT NULL SELECT A.INST_CD /* 기관코드 */,
    A.NTFCTN_SECD /* 통보구분코드 */,
    A.NTFCTN_SQNO /* 통보순번 */,
    A.FRS_MDM_SQNO /* 포렌식매체순번 */,
    A.CHG_RRN /* 변경주민등록번호 */
FROM TBCSPBII430L A /* 통보사항관계자임시내역 */ WHERE A.CHG_RRN IS NOT NULL SELECT A.INST_CD /* 기관코드 */,
    A.CVLCPT_ADT_YR /* 민원감사년도 */,
    A.ADT_SQNO /* 감사순번 */,
    A.EPFC_MNG_SQNO /* 사후관리순번 */,
    A.FRS_MDM_SQNO /* 포렌식매체순번 */,
    A.CHG_RRN /* 변경주민등록번호 */
FROM TBCSPBII440L A /* 자체감사처분요구임시내역 */ WHERE A.CHG_RRN IS NOT NULL