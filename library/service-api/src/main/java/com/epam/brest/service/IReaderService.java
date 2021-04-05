package com.epam.brest.service;


import com.epam.brest.model.sample.ReaderSample;

public interface IReaderService {

    /**
     * Find a reader.
     * @param id reader identification.
     * @return sample of the reader with books.
     */
    ReaderSample getProfile(Integer id);
    /**
     * Find a reader.
     * @param id reader identification.
     * @return sample of the reader without books.
     */
    ReaderSample getProfileWithoutBooks(Integer id);
    /**
     * Create and save a reader.
     * @param readerSample sample of the reader.
     * @return sample of the saved reader.
     */
    ReaderSample createReader(ReaderSample readerSample);

    /**
     * Update a reader.
     * @param readerSample sample of the reader who is to updated.
     * @return true if executed and false if not executed.
     */
    Boolean editProfile(ReaderSample readerSample);

    /**
     * Set a reader not active
     * @param id reader identification.
     * @return true if executed and false if not executed.
     */
    Boolean removeProfile(Integer id);

    /**
     * Set a reader active
     * @param id reader identification.
     * @return true if executed and false if not executed.
     */
    Boolean restoreProfile(Integer id);


}
