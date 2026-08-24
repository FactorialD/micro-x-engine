package com.microx.engine.save;
/** Minimal append-only backend, allowing the transaction algorithm to be desktop-tested. */
public interface RecordBackend {int add(byte[] data)throws Exception;int[] ids()throws Exception;byte[] get(int id)throws Exception;void close()throws Exception;}
