SELECT A.REG_DMND_YMD /* 등록요청일자 */,
    B.USER_NM /* 사용자명 */,
    A.SBMSN_INST_NM /* 제출기관명 */,
    A.ADT_SBMSN_DATA_NM /* 감사제출자료명 */,
    A.SBMSN_YMD /* 제출일자 */,
    A.PRSNR_NM /* 제출자명 */,
    A.DMND_INST_CNTN_YMD /* 요청기관접속일자 */,
    A.AUTHRT_SECD /* 권한구분코드 */,
    C.FILE_HASH_VL /* 파일해시값 */,
    C.FILE_SIGN_VL /* 파일서명값 */
FROM TB_DCMIDM001L A ,TBDCMACM001M B ,TB_DCMIDM001D C WHERE A.RQSTR_ENO = B.TRGT_NOP_ENO(+) AND A.ADT_SBMSN_DATA_ID = C.ADT_SBMSN_DATA_ID AND C.FILE_HASH_VL = #FILE_HASH_VL#