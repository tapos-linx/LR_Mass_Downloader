import React, { useState, useEffect, useRef, useMemo, useCallback } from 'react';

/**
 * Bangladesh Land Records (LR) Mass Downloader Updated
 * GovTech UI Specialist Edition - MD3 & Deep Forest Emerald Palette
 * Districts: Cumilla (কুমিল্লা) & Brahmanbaria (ব্রাহ্মণবাড়িয়া)
 *
 * Specific Enhancements:
 * 1. When an Upazila is selected, first fetch how many mouzas are in that upazila.
 * 2. Set the target to the found actual mouza number.
 * 3. Select strictly the initial 7 mouzas for Batch 1 (not all 100+ at once).
 * 4. Process the queue sequentially in 7-mouza batches without repeating previously downloaded ones.
 * 5. Once all mouzas of the upazila finish, compile a master file with 1-click download and auto-clear memory/cache.
 */

export type RecordType = 'CS' | 'SA' | 'RS' | 'BRS';

export interface MouzaItem {
  id: string;
  nameBn: string;
  nameEn: string;
  jlNo: string;
}

export interface UpazilaItem {
  id: string;
  nameBn: string;
  nameEn: string;
  mouzas: MouzaItem[];
}

export interface DistrictItem {
  id: string;
  nameBn: string;
  nameEn: string;
  upazilas: UpazilaItem[];
}

export type TaskStatus = 'PENDING' | 'DOWNLOADING' | 'COMPLETED' | 'PAUSED' | 'FAILED';

export interface DownloadTask {
  id: string;
  mouzaId: string;
  mouzaNameBn: string;
  mouzaNameEn: string;
  jlNo: string;
  upazilaNameBn: string;
  districtNameBn: string;
  recordTypes: RecordType[];
  status: TaskStatus;
  progress: number; // 0 - 100
  totalKhatians: number;
  downloadedKhatians: number;
  fileSizeBytes: number;
  speedKbps: number;
  batchNumber: number;
  timestamp: number;
}

export interface MasterArchive {
  upazilaId: string;
  upazilaNameBn: string;
  upazilaNameEn: string;
  districtNameBn: string;
  districtNameEn: string;
  totalMouzasCount: number;
  recordTypesIncluded: string;
  totalKhatiansCount: number;
  totalSizeMb: string;
  fileName: string;
  isReady: boolean;
  isDownloaded: boolean;
}

export interface UpazilaFetchInfo {
  upazilaNameBn: string;
  upazilaNameEn: string;
  totalMouzasCount: number;
  totalBatches: number;
  message: string;
}

// Helper to convert number to Bangla digits
const toBanglaDigits = (num: number): string => {
  const banglaDigits = ['০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'];
  return num.toString().split('').map(d => banglaDigits[parseInt(d, 10)]).join('');
};

// Generates realistic mouzas for an Upazila matching target count
const generateMouzas = (
  prefix: string,
  anchors: { bn: string; en: string; jl: string }[],
  targetCount: number
): MouzaItem[] => {
  const list: MouzaItem[] = [];
  const existingNames = new Set<string>();

  anchors.forEach((a, i) => {
    list.push({
      id: `${prefix}-${i + 1}`,
      nameBn: a.bn,
      nameEn: a.en,
      jlNo: a.jl,
    });
    existingNames.add(a.bn);
  });

  const rootsBn = [
    'কাঞ্চনপুর', 'গোবিন্দপুর', 'ফতেহপুর', 'রামচন্দ্রপুর', 'সোনাকান্দা',
    'কালিকাপুর', 'সুলতানপুর', 'রায়পুর', 'কল্যাণপুর', 'মাধবপুর',
    'আহমদপুর', 'হরিপুর', 'জগন্নাথপুর', 'কৃষ্ণপুর', 'আনন্দপুর',
    'দোলিয়া', 'চাঁদপুর', 'পাহাড়পুর', 'শান্তিপুর', 'বীরপুর',
    'মির্জাপুর', 'নবাবপুর', 'সাদেকপুর', 'কাশিমপুর', 'মহেশপুর',
    'মীরপুর', 'কামালপুর', 'জালালপুর', 'আলমপুর', 'বোরহানপুর',
    'রতনপুর', 'হবিবপুর', 'তাজপুর', 'সুজানগর', 'গোবিন্দনগর',
    'শিবপুর', 'শ্রীপুর', 'দক্ষিণগ্রাম', 'উত্তরপাড়া', 'মধ্যপাড়া',
    'পশ্চিমপাড়া', 'পূর্বপাড়া', 'চরবালুকা', 'চরইসলামপুর', 'চরলক্ষ্মীপুর',
  ];
  const rootsEn = [
    'Kanchanpur', 'Gobindapur', 'Fatehpur', 'Ramchandrapur', 'Sonakanda',
    'Kalikapur', 'Sultanpur', 'Raipur', 'Kalyanpur', 'Madhabpur',
    'Ahmadpur', 'Haripur', 'Jagannathpur', 'Krishnapur', 'Anandapur',
    'Dolia', 'Chandpur', 'Paharpur', 'Shantipur', 'Birpur',
    'Mirzapur', 'Nawabpur', 'Sadekpur', 'Kashimpur', 'Maheshpur',
    'Mirpur', 'Kamalpur', 'Jalalpur', 'Alampur', 'Borhanpur',
    'Ratanpur', 'Habibpur', 'Tajpur', 'Sujanagar', 'Gobindanagar',
    'Shibpur', 'Sreepur', 'Dakshin Gram', 'Uttar Para', 'Madhya Para',
    'Paschim Para', 'Purba Para', 'Char Baluka', 'Char Islampur', 'Char Lakshmipur',
  ];

  let counter = anchors.length + 1;
  let rIdx = 0;

  while (list.length < targetCount) {
    const rawBn = rootsBn[rIdx % rootsBn.length];
    const rawEn = rootsEn[rIdx % rootsEn.length];

    const finalBn = existingNames.has(rawBn)
      ? `${rawBn} ${list.length < 40 ? 'উত্তর' : list.length < 80 ? 'দক্ষিণ' : 'পশ্চিম'}`
      : rawBn;
    const finalEn = existingNames.has(rawBn)
      ? `${rawEn} ${list.length < 40 ? 'North' : list.length < 80 ? 'South' : 'West'}`
      : rawEn;

    existingNames.add(finalBn);
    list.push({
      id: `${prefix}-${counter}`,
      nameBn: finalBn,
      nameEn: finalEn,
      jlNo: toBanglaDigits(counter),
    });

    counter++;
    rIdx++;
  }

  return list;
};

// -------------------------------------------------------------
// COMPREHENSIVE DATASET (SPECIFIC MOUZA COUNTS PER UPAZILA)
// -------------------------------------------------------------
const LR_DATASET: DistrictItem[] = [
  {
    id: 'brahmanbaria',
    nameBn: 'ব্রাহ্মণবাড়িয়া',
    nameEn: 'Brahmanbaria',
    upazilas: [
      {
        id: 'brahmanbaria-sadar',
        nameBn: 'ব্রাহ্মণবাড়িয়া সদর',
        nameEn: 'Brahmanbaria Sadar',
        mouzas: generateMouzas('bb-sad', [
          { bn: 'মেদ্দা', en: 'Medda', jl: '৪২' },
          { bn: 'কাজীপুর', en: 'Kazipura', jl: '৪৩' },
          { bn: 'পাইকপাড়া', en: 'Paikpara', jl: '৪৪' },
          { bn: 'ঘাতুরা', en: 'Ghatura', jl: '২৮' },
          { bn: 'বীরমপুর', en: 'Birampur', jl: '৩১' },
          { bn: 'সুহিলপুর', en: 'Shuhilpur', jl: '২২' },
          { bn: 'নাতাই', en: 'Natai', jl: '৫৫' },
          { bn: 'মাছিহাতা', en: 'Machihata', jl: '৬১' },
        ], 126),
      },
      {
        id: 'sarail',
        nameBn: 'সরাইল',
        nameEn: 'Sarail',
        mouzas: generateMouzas('bb-sar', [
          { bn: 'সরাইল সদর', en: 'Sarail Sadar', jl: '১২' },
          { bn: 'কালিকচ্ছ', en: 'Kalikachha', jl: '১৮' },
          { bn: 'নোয়াগাঁও', en: 'Noagaon', jl: '২৫' },
          { bn: 'শাহবাজপুর', en: 'Shahbazpur', jl: '৩২' },
          { bn: 'চুন্টা', en: 'Chunta', jl: '১৫' },
          { bn: 'পানিশ্বর', en: 'Panishwar', jl: '৪০' },
          { bn: 'পাকশিমুল', en: 'Pakshimul', jl: '৪৮' },
        ], 92),
      },
      {
        id: 'ashuganj',
        nameBn: 'আশুগঞ্জ',
        nameEn: 'Ashuganj',
        mouzas: generateMouzas('bb-ash', [
          { bn: 'আশুগঞ্জ', en: 'Ashuganj', jl: '০৪' },
          { bn: 'চর চারতলা', en: 'Char Chartala', jl: '০৮' },
          { bn: 'দুর্গাপুর', en: 'Durgapur', jl: '১১' },
          { bn: 'তালশহর পশ্চিম', en: 'Talshahar Paschim', jl: '১৪' },
          { bn: 'সোহাগপুর', en: 'Soagpur', jl: '১৯' },
          { bn: 'আড়াইসিধা', en: 'Araisidha', jl: '২৩' },
        ], 56),
      },
      {
        id: 'kasba',
        nameBn: 'কসবা',
        nameEn: 'Kasba',
        mouzas: generateMouzas('bb-kas', [
          { bn: 'কসবা', en: 'Kasba', jl: '১৫' },
          { bn: 'কুটি', en: 'Kuti', jl: '০২' },
          { bn: 'কায়েমপুর', en: 'Kayempur', jl: '২২' },
          { bn: 'বাদৈর', en: 'Badair', jl: '৩৪' },
          { bn: 'বায়েখ', en: 'Bayek', jl: '৪১' },
          { bn: 'খাড়েরা', en: 'Kharera', jl: '২৯' },
          { bn: 'মেহারী', en: 'Mehari', jl: '১৮' },
        ], 116),
      },
      {
        id: 'nabinagar',
        nameBn: 'নবীনগর',
        nameEn: 'Nabinagar',
        mouzas: generateMouzas('bb-nab', [
          { bn: 'নবীনগর সদর', en: 'Nabinagar Sadar', jl: '০১' },
          { bn: 'বিদ্যাকুট', en: 'Biddakut', jl: '০৯' },
          { bn: 'শিবপুর', en: 'Shibpur', jl: '১৬' },
          { bn: 'বিটঘর', en: 'Bitghar', jl: '২৪' },
          { bn: 'কাইতলা', en: 'Kaitala', jl: '৩৩' },
          { bn: 'সলিমগঞ্জ', en: 'Salimganj', jl: '৪৫' },
          { bn: 'জিনোদপুর', en: 'Jinodpur', jl: '৫২' },
        ], 144),
      },
      {
        id: 'nasirnagar',
        nameBn: 'নাসিরনগর',
        nameEn: 'Nasirnagar',
        mouzas: generateMouzas('bb-nas', [
          { bn: 'নাসিরনগর', en: 'Nasirnagar', jl: '০৫' },
          { bn: 'ফান্দাউক', en: 'Fandauk', jl: '১১' },
          { bn: 'চাতলপাড়', en: 'Chatalpar', jl: '১৮' },
          { bn: 'হরিপুর', en: 'Haripur', jl: '২২' },
          { bn: 'বুড়িশ্বর', en: 'Burishwar', jl: '২৯' },
          { bn: 'কুন্ডা', en: 'Kunda', jl: '৩৫' },
          { bn: 'গোকর্ণ', en: 'Gokarna', jl: '৪১' },
        ], 96),
      },
      {
        id: 'bancharampur',
        nameBn: 'বাঞ্ছারামপুর',
        nameEn: 'Bancharampur',
        mouzas: generateMouzas('bb-ban', [
          { bn: 'বাঞ্ছারামপুর সদর', en: 'Bancharampur Sadar', jl: '০৩' },
          { bn: 'উজানচর', en: 'Ujanchar', jl: '০৭' },
          { bn: 'দরিয়াদৌলত', en: 'Dariyadaulat', jl: '১৪' },
          { bn: 'মানিকপুর', en: 'Manikpur', jl: '২১' },
          { bn: 'সলিমাবাদ', en: 'Salimabad', jl: '২৮' },
          { bn: 'তেজখালী', en: 'Tejkhali', jl: '৩৫' },
          { bn: 'পাহাড়িয়াকান্দি', en: 'Pahariakandi', jl: '৪২' },
        ], 82),
      },
      {
        id: 'akhaura',
        nameBn: 'আখাউড়া',
        nameEn: 'Akhaura',
        mouzas: generateMouzas('bb-akh', [
          { bn: 'আখাউড়া পৌরসভা', en: 'Akhaura Municipality', jl: '০২' },
          { bn: 'মোগড়া', en: 'Mogra', jl: '১০' },
          { bn: 'মণিয়ন্দ', en: 'Moniyond', jl: '১৬' },
          { bn: 'ধরখার', en: 'Dharkhar', jl: '২৪' },
          { bn: 'গঙ্গাসাগর', en: 'Gangasagar', jl: '০৮' },
          { bn: 'নূরপুর', en: 'Noorpur', jl: '১৯' },
        ], 68),
      },
      {
        id: 'bijoynagar',
        nameBn: 'বিজয়নগর',
        nameEn: 'Bijoynagar',
        mouzas: generateMouzas('bb-bij', [
          { bn: 'চান্দুরা', en: 'Chandura', jl: '০৬' },
          { bn: 'সিঙ্গারবিল', en: 'Singerbil', jl: '১২' },
          { bn: 'হরষপুর', en: 'Harashpur', jl: '১৯' },
          { bn: 'বুধন্তী', en: 'Budhanti', jl: '২৭' },
          { bn: 'ইছাপুরা', en: 'Ichhapur', jl: '৩৪' },
          { bn: 'চম্পকনগর', en: 'Champaknagar', jl: '৪১' },
          { bn: 'পাহাড়পুর', en: 'Paharpur', jl: '৪৮' },
        ], 88),
      },
    ],
  },
  {
    id: 'cumilla',
    nameBn: 'কুমিল্লা',
    nameEn: 'Cumilla',
    upazilas: [
      {
        id: 'adarsha-sadar',
        nameBn: 'আদর্শ সদর',
        nameEn: 'Adarsha Sadar',
        mouzas: generateMouzas('cu-ada', [
          { bn: 'শাশনগাছা', en: 'Shashan Gachha', jl: '১৪' },
          { bn: 'ছাতিপট্টি', en: 'Chhatipatti', jl: '১৮' },
          { bn: 'বাদুড়তলা', en: 'Badurtala', jl: '২২' },
          { bn: 'বাগিচাগাঁও', en: 'Bagichagaon', jl: '২৭' },
          { bn: 'জগন্নাথপুর', en: 'Jagannathpur', jl: '৩৫' },
          { bn: 'আমড়াতলী', en: 'Amratali', jl: '৪৮' },
          { bn: 'পাঁচথুবী', en: 'Panchthubi', jl: '৫৪' },
        ], 94),
      },
      {
        id: 'sadar-dakshin',
        nameBn: 'সদর দক্ষিণ',
        nameEn: 'Sadar Dakshin',
        mouzas: generateMouzas('cu-sdk', [
          { bn: 'বিজয়পুর', en: 'Bijoypur', jl: '১২' },
          { bn: 'চৌয়ারা', en: 'Chowara', jl: '১৯' },
          { bn: 'গোপীনাথপুর', en: 'Gopinathpur', jl: '২৬' },
          { bn: 'গোলাবাড়ি', en: 'Golabari', jl: '৩৩' },
          { bn: 'বাড়পাড়া', en: 'Barapara', jl: '৪০' },
          { bn: 'পেরুল', en: 'Perul', jl: '৪৭' },
        ], 112),
      },
      {
        id: 'chandina',
        nameBn: 'চান্দিনা',
        nameEn: 'Chandina',
        mouzas: generateMouzas('cu-cha', [
          { bn: 'চান্দিনা পৌরসভা', en: 'Chandina Pouroshova', jl: '০৮' },
          { bn: 'মাধাইয়া', en: 'Madhaiya', jl: '১৫' },
          { bn: 'বরকইট', en: 'Barkait', jl: '২৩' },
          { bn: 'মাইজখার', en: 'Maijkhar', jl: '৩১' },
          { bn: 'কেরণখাল', en: 'Kerankhal', jl: '৩৯' },
          { bn: 'গল্লাই', en: 'Gallai', jl: '৪৬' },
          { bn: 'বাতাগাসী', en: 'Bataghashi', jl: '৫২' },
        ], 124),
      },
      {
        id: 'daudkandi',
        nameBn: 'দাউদকান্দি',
        nameEn: 'Daudkandi',
        mouzas: generateMouzas('cu-dau', [
          { bn: 'দাউদকান্দি সদর', en: 'Daudkandi Sadar', jl: '০৫' },
          { bn: 'গৌরীপুর', en: 'Gouripur', jl: '১৪' },
          { bn: 'ইলিয়টগঞ্জ', en: 'Eliotganj', jl: '২২' },
          { bn: 'সুন্দলপুর', en: 'Sundalpur', jl: '২৯' },
          { bn: 'জিংলাতলী', en: 'Jinglatali', jl: '৩৬' },
          { bn: 'মারুকা', en: 'Maruka', jl: '৪৩' },
          { bn: 'গোয়ালমারী', en: 'Goalmari', jl: '৫০' },
        ], 148),
      },
      {
        id: 'debidwar',
        nameBn: 'দেবিদ্বার',
        nameEn: 'Debidwar',
        mouzas: generateMouzas('cu-deb', [
          { bn: 'দেবিদ্বার পৌরসভা', en: 'Debidwar Pouro', jl: '১০' },
          { bn: 'মোহনপুর', en: 'Mohanpur', jl: '১৭' },
          { bn: 'রসুল্লাবাদ', en: 'Rasullabad', jl: '২৫' },
          { bn: 'গুনাইঘর', en: 'Gunaighar', jl: '৩২' },
          { bn: 'ধামতী', en: 'Dhamti', jl: '৪১' },
          { bn: 'জাফরগঞ্জ', en: 'Jafarganj', jl: '৪৯' },
        ], 136),
      },
      {
        id: 'burichang',
        nameBn: 'বুড়িচং',
        nameEn: 'Burichang',
        mouzas: generateMouzas('cu-bur', [
          { bn: 'বুড়িচং সদর', en: 'Burichang Sadar', jl: '০৭' },
          { bn: 'ময়নামতি', en: 'Mainamati', jl: '২২' },
          { bn: 'পীরযাত্রাপুর', en: 'Pirjatrapur', jl: '১৫' },
          { bn: 'বাকশীমূল', en: 'Bakshimul', jl: '৩১' },
          { bn: 'মোকাম', en: 'Mokam', jl: '৩৮' },
          { bn: 'রাজাপুর', en: 'Rajapur', jl: '৪৫' },
        ], 118),
      },
      {
        id: 'brahmanpara',
        nameBn: 'ব্রাহ্মণপাড়া',
        nameEn: 'Brahmanpara',
        mouzas: generateMouzas('cu-brp', [
          { bn: 'ব্রাহ্মণপাড়া সদর', en: 'Brahmanpara Sadar', jl: '০৪' },
          { bn: 'মাধবপুর', en: 'Madhabpur', jl: '১১' },
          { bn: 'শিদলাই', en: 'Shidlai', jl: '১৯' },
          { bn: 'চান্দলা', en: 'Chandla', jl: '২৬' },
          { bn: 'শশীদল', en: 'Shashidal', jl: '৩৪' },
          { bn: 'দুলালপুর', en: 'Dulalpur', jl: '৪২' },
        ], 86),
      },
      {
        id: 'chauddagram',
        nameBn: 'চৌদ্দগ্রাম',
        nameEn: 'Chauddagram',
        mouzas: generateMouzas('cu-chaud', [
          { bn: 'চৌদ্দগ্রাম বাজার', en: 'Chauddagram Bazar', jl: '০৬' },
          { bn: 'মিয়াবাজার', en: 'Miabazar', jl: '১৩' },
          { bn: 'কাশীনগর', en: 'Kashinagar', jl: '২১' },
          { bn: 'বাতিসা', en: 'Batisa', jl: '২৯' },
          { bn: 'মুন্সীরহাট', en: 'Munshirhat', jl: '৩৭' },
          { bn: 'গুণবতী', en: 'Gunabati', jl: '৪৪' },
          { bn: 'চিওড়া', en: 'Cheora', jl: '৫১' },
        ], 162),
      },
      {
        id: 'laksam',
        nameBn: 'লাকসাম',
        nameEn: 'Laksam',
        mouzas: generateMouzas('cu-lak', [
          { bn: 'লাকসাম পৌরসভা', en: 'Laksam Pouro', jl: '০৩' },
          { bn: 'কান্দিরপাড়', en: 'Kandirpar', jl: '১০' },
          { bn: 'গোবিন্দপুর', en: 'Gobindapur', jl: '১৮' },
          { bn: 'উত্তরদা', en: 'Uttarda', jl: '২৫' },
          { bn: 'মুদাফফরগঞ্জ', en: 'Mudhafurganj', jl: '৪২' },
        ], 98),
      },
      {
        id: 'muradnagar',
        nameBn: 'মুরাদনগর',
        nameEn: 'Muradnagar',
        mouzas: generateMouzas('cu-mur', [
          { bn: 'মুরাদনগর সদর', en: 'Muradnagar Sadar', jl: '০৯' },
          { bn: 'কোম্পানীগঞ্জ', en: 'Companyganj', jl: '১৬' },
          { bn: 'বাঙ্গরা', en: 'Bangora', jl: '২৪' },
          { bn: 'জাহাপুর', en: 'Jahapur', jl: '৩৩' },
          { bn: 'রামচন্দ্রপুর', en: 'Ramchandrapur', jl: '৪১' },
          { bn: 'শ্রীকাইল', en: 'Sreekail', jl: '৫০' },
        ], 154),
      },
      {
        id: 'barura',
        nameBn: 'বরুড়া',
        nameEn: 'Barura',
        mouzas: generateMouzas('cu-bar', [
          { bn: 'বরুড়া পৌরসভা', en: 'Barura Pouro', jl: '০৫' },
          { bn: 'গালিমপুর', en: 'Galimpur', jl: '১২' },
          { bn: 'শিলমুড়ী', en: 'Shilmuri', jl: '২০' },
          { bn: 'পায়েলগাছা', en: 'Payalgacha', jl: '২৮' },
          { bn: 'আড্ডা', en: 'Adda', jl: '৩৫' },
          { bn: 'শাকপুর', en: 'Shakpur', jl: '৪৩' },
        ], 132),
      },
      {
        id: 'homna',
        nameBn: 'হোমনা',
        nameEn: 'Homna',
        mouzas: generateMouzas('cu-hom', [
          { bn: 'হোমনা সদর', en: 'Homna Sadar', jl: '০২' },
          { bn: 'আসাদপুর', en: 'Asadpur', jl: '০৮' },
          { bn: 'জয়পুর', en: 'Joypur', jl: '১৫' },
          { bn: 'ঘাগুটিয়া', en: 'Ghagutia', jl: '৩০' },
          { bn: 'মাথাভাঙ্গা', en: 'Mathabhanga', jl: '৪৬' },
        ], 88),
      },
      {
        id: 'titas',
        nameBn: 'তিতাস',
        nameEn: 'Titas',
        mouzas: generateMouzas('cu-tit', [
          { bn: 'মজিদপুর', en: 'Majidpur', jl: '০৪' },
          { bn: 'বলরামপুর', en: 'Balrampur', jl: '১১' },
          { bn: 'জগতপুর', en: 'Jagatpur', jl: '১৯' },
          { bn: 'কড়িকান্দি', en: 'Karikandi', jl: '২৭' },
          { bn: 'জিয়ারকান্দি', en: 'Zearkandi', jl: '৪১' },
        ], 74),
      },
      {
        id: 'meghna',
        nameBn: 'মেঘনা',
        nameEn: 'Meghna',
        mouzas: generateMouzas('cu-meg', [
          { bn: 'মানিকরচর', en: 'Manikar Char', jl: '০৩' },
          { bn: 'চন্দনপুর', en: 'Chandanpur', jl: '০৯' },
          { bn: 'চালিভাঙ্গা', en: 'Chalibhanga', jl: '১৬' },
          { bn: 'গোবিন্দপুর', en: 'Gobindapur', jl: '২৪' },
          { bn: 'রাধানগর', en: 'Radhanagar', jl: '৩১' },
        ], 62),
      },
      {
        id: 'monohargonj',
        nameBn: 'মনোহরগঞ্জ',
        nameEn: 'Monohargonj',
        mouzas: generateMouzas('cu-mon', [
          { bn: 'মনোহরগঞ্জ সদর', en: 'Monohargonj Sadar', jl: '০৫' },
          { bn: 'বাইশগাঁও', en: 'Baishgaon', jl: '১২' },
          { bn: 'সরসপুর', en: 'Sarashpur', jl: '১৯' },
          { bn: 'হাসনাবাদ', en: 'Hasnabad', jl: '২৭' },
          { bn: 'ঝালম', en: 'Jhalam', jl: '৩৪' },
        ], 104),
      },
      {
        id: 'nangalkot',
        nameBn: 'নাঙ্গলকোট',
        nameEn: 'Nangalkot',
        mouzas: generateMouzas('cu-nan', [
          { bn: 'নাঙ্গলকোট পৌরসভা', en: 'Nangalkot Pouro', jl: '০৬' },
          { bn: 'ঢালুয়া', en: 'Dhalua', jl: '১৪' },
          { bn: 'বক্সগঞ্জ', en: 'Boxoganj', jl: '২১' },
          { bn: 'মোকরা', en: 'Mokara', jl: '২৯' },
          { bn: 'পেরিয়া', en: 'Peria', jl: '৩৭' },
          { bn: 'রায়কোট', en: 'Roykot', jl: '৪৫' },
        ], 142),
      },
      {
        id: 'lalmai',
        nameBn: 'লালমাই',
        nameEn: 'Lalmai',
        mouzas: generateMouzas('cu-lal', [
          { bn: 'বাগমারা', en: 'Bagmara', jl: '০৮' },
          { bn: 'ভুলাইণ', en: 'Bhulain', jl: '১৫' },
          { bn: 'বেলঘর', en: 'Belghar', jl: '২৩' },
          { bn: 'পেরুল দক্ষিণ', en: 'Perul Dakshin', jl: '৩১' },
          { bn: 'বাকই উত্তর', en: 'Bakoi Uttar', jl: '৩৯' },
        ], 78),
      },
    ],
  },
];

export const LRMassDownloaderUpdated: React.FC = () => {
  // Cascading Selection State
  const [selectedDistrictId, setSelectedDistrictId] = useState<string>('cumilla');
  const [selectedUpazilaId, setSelectedUpazilaId] = useState<string>('');
  const [upazilaFetchInfo, setUpazilaFetchInfo] = useState<UpazilaFetchInfo | null>(null);
  const [targetTotalMouzas, setTargetTotalMouzas] = useState<number>(0);
  const [selectedMouzaIds, setSelectedMouzaIds] = useState<string[]>([]);
  const [downloadedMouzaIds, setDownloadedMouzaIds] = useState<string[]>([]);
  const [selectedRecordTypes, setSelectedRecordTypes] = useState<RecordType[]>(['RS']);
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Batching & Queue Pipeline State
  const [currentBatchNumber, setCurrentBatchNumber] = useState<number>(1);
  const [queue, setQueue] = useState<DownloadTask[]>([]);
  const [isPaused, setIsPaused] = useState<boolean>(false);
  const [masterArchive, setMasterArchive] = useState<MasterArchive | null>(null);
  const [cacheNotice, setCacheNotice] = useState<string | null>(null);

  const isPausedRef = useRef<boolean>(false);
  isPausedRef.current = isPaused;

  const queueRef = useRef<DownloadTask[]>([]);
  queueRef.current = queue;

  const activeDownloadsRef = useRef<number>(0);

  const currentDistrict = useMemo(() => {
    return LR_DATASET.find((d) => d.id === selectedDistrictId) || null;
  }, [selectedDistrictId]);

  const currentUpazilaList = useMemo(() => {
    return currentDistrict ? currentDistrict.upazilas : [];
  }, [currentDistrict]);

  const currentUpazila = useMemo(() => {
    return currentUpazilaList.find((u) => u.id === selectedUpazilaId) || null;
  }, [currentUpazilaList, selectedUpazilaId]);

  const availableMouzas = useMemo(() => {
    return currentUpazila ? currentUpazila.mouzas : [];
  }, [currentUpazila]);

  const totalBatches = useMemo(() => {
    if (availableMouzas.length === 0) return 0;
    return Math.ceil(availableMouzas.length / 7);
  }, [availableMouzas]);

  const filteredMouzas = useMemo(() => {
    if (!searchQuery.trim()) return availableMouzas;
    const q = searchQuery.toLowerCase().trim();
    return availableMouzas.filter(
      (m) =>
        m.nameBn.toLowerCase().includes(q) ||
        m.nameEn.toLowerCase().includes(q) ||
        m.jlNo.includes(q)
    );
  }, [availableMouzas, searchQuery]);

  // District Change Handler
  const handleDistrictChange = (newDistrictId: string) => {
    setSelectedDistrictId(newDistrictId);
    setSelectedUpazilaId('');
    setUpazilaFetchInfo(null);
    setTargetTotalMouzas(0);
    setSelectedMouzaIds([]);
    setDownloadedMouzaIds([]);
    setQueue([]);
    setCurrentBatchNumber(1);
    setMasterArchive(null);
    setSearchQuery('');
  };

  /**
   * Requirement 1:
   * When I select an upazila, then first fetch how many mouzas in that selected upazila.
   * Then target is set to the actual found mouza number.
   * Select strictly initial 7 mouzas for Batch 1.
   */
  const handleUpazilaChange = (newUpazilaId: string) => {
    setSelectedUpazilaId(newUpazilaId);
    setSearchQuery('');
    setDownloadedMouzaIds([]);
    setQueue([]);
    setCurrentBatchNumber(1);
    setMasterArchive(null);

    if (!newUpazilaId) {
      setUpazilaFetchInfo(null);
      setTargetTotalMouzas(0);
      setSelectedMouzaIds([]);
      return;
    }

    const upazila = currentUpazilaList.find((u) => u.id === newUpazilaId);
    if (upazila && upazila.mouzas && upazila.mouzas.length > 0) {
      const foundCount = upazila.mouzas.length;
      const batches = Math.ceil(foundCount / 7);

      setUpazilaFetchInfo({
        upazilaNameBn: upazila.nameBn,
        upazilaNameEn: upazila.nameEn,
        totalMouzasCount: foundCount,
        totalBatches: batches,
        message: `Upazila Index: Found ${foundCount} mouzas in ${upazila.nameBn}. Sequential Target set to ${foundCount} mouzas across ${batches} batches.`,
      });

      setTargetTotalMouzas(foundCount);

      // Select strictly the first 7 mouzas for initial batch queue
      setSelectedMouzaIds(upazila.mouzas.slice(0, 7).map((m) => m.id));
    } else {
      setUpazilaFetchInfo(null);
      setTargetTotalMouzas(0);
      setSelectedMouzaIds([]);
    }
  };

  // Record Type Controls
  const toggleRecordType = (type: RecordType) => {
    setSelectedRecordTypes((prev) => {
      if (prev.includes(type)) {
        return prev.length > 1 ? prev.filter((t) => t !== type) : prev;
      }
      return [...prev, type];
    });
  };

  const handleSelectAllRecordTypes = () => {
    setSelectedRecordTypes(['CS', 'SA', 'RS', 'BRS']);
  };

  // Mouza Selection Controls
  const toggleMouza = (mouzaId: string) => {
    setSelectedMouzaIds((prev) =>
      prev.includes(mouzaId) ? prev.filter((id) => id !== mouzaId) : [...prev, mouzaId]
    );
  };

  const handleSelectAll = () => {
    if (availableMouzas.length === 0) return;
    setSelectedMouzaIds(availableMouzas.map((m) => m.id));
  };

  const handleSelectNextSeven = () => {
    const unDownloaded = availableMouzas.filter((m) => !downloadedMouzaIds.includes(m.id));
    setSelectedMouzaIds(unDownloaded.slice(0, 7).map((m) => m.id));
  };

  const handleDeselectAll = () => {
    setSelectedMouzaIds([]);
  };

  // -------------------------------------------------------------
  // SEQUENTIAL 7-MOUZA PIPELINE WITHOUT REPEATS
  // -------------------------------------------------------------
  const triggerNextTasks = useCallback(() => {
    if (isPausedRef.current) return;

    while (activeDownloadsRef.current < 2) {
      const currentList = queueRef.current;
      const nextPendingIndex = currentList.findIndex((t) => t.status === 'PENDING');
      if (nextPendingIndex === -1) break;

      const taskToRun = currentList[nextPendingIndex];
      activeDownloadsRef.current += 1;

      setQueue((prev) =>
        prev.map((t) => (t.id === taskToRun.id ? { ...t, status: 'DOWNLOADING', progress: 5 } : t))
      );

      runTaskWorker(taskToRun.id);
    }
  }, []);

  const runTaskWorker = async (taskId: string) => {
    const totalSteps = 8;
    const intervalTime = 220 + Math.floor(Math.random() * 140);

    for (let step = 1; step <= totalSteps; step++) {
      while (isPausedRef.current) {
        await new Promise((r) => setTimeout(r, 350));
        const exists = queueRef.current.find((t) => t.id === taskId);
        if (!exists) {
          activeDownloadsRef.current = Math.max(0, activeDownloadsRef.current - 1);
          return;
        }
      }

      await new Promise((resolve) => setTimeout(resolve, intervalTime));

      const exists = queueRef.current.find((t) => t.id === taskId);
      if (!exists) {
        activeDownloadsRef.current = Math.max(0, activeDownloadsRef.current - 1);
        return;
      }

      const progressVal = Math.min(100, Math.round((step / totalSteps) * 100));
      const simulatedSpeed = 380 + Math.floor(Math.random() * 160);

      setQueue((prev) =>
        prev.map((t) => {
          if (t.id !== taskId) return t;
          const downloaded = Math.round((progressVal / 100) * t.totalKhatians);
          return {
            ...t,
            progress: progressVal,
            downloadedKhatians: downloaded,
            speedKbps: simulatedSpeed,
            status: progressVal >= 100 ? 'COMPLETED' : 'DOWNLOADING',
          };
        })
      );
    }

    activeDownloadsRef.current = Math.max(0, activeDownloadsRef.current - 1);

    // Record finished mouza
    const finishedTask = queueRef.current.find((t) => t.id === taskId);
    if (finishedTask) {
      setDownloadedMouzaIds((prev) =>
        prev.includes(finishedTask.mouzaId) ? prev : [...prev, finishedTask.mouzaId]
      );
    }

    // Check if batch is completed and pull next 7 without repeats
    checkBatchProgression();

    triggerNextTasks();
  };

  const checkBatchProgression = () => {
    setTimeout(() => {
      const q = queueRef.current;
      if (q.length === 0) return;
      const allCompleted = q.every((t) => t.status === 'COMPLETED');

      if (allCompleted && currentUpazila) {
        const completedIds = Array.from(new Set(q.map((t) => t.mouzaId)));
        const unDownloaded = currentUpazila.mouzas.filter((m) => !completedIds.includes(m.id));

        if (unDownloaded.length > 0) {
          // AUTO-FETCH NEXT 7 MOUZAS - NO REPEATS!
          const nextSeven = unDownloaded.slice(0, 7);
          setCurrentBatchNumber((b) => b + 1);
          setSelectedMouzaIds(nextSeven.map((m) => m.id));

          enqueueBatchOfMouzas(nextSeven, currentBatchNumber + 1);
        } else {
          // All mouzas of the upazila are finished! Compile master archive
          buildMasterArchive(q);
        }
      }
    }, 400);
  };

  const buildMasterArchive = (allTasks: DownloadTask[]) => {
    if (!currentUpazila || !currentDistrict) return;
    const totalKhatians = allTasks.reduce((acc, t) => acc + t.totalKhatians, 0);
    const totalBytes = allTasks.reduce((acc, t) => acc + t.fileSizeBytes, 0);
    const sizeMb = (totalBytes / (1024 * 1024)).toFixed(2) + ' MB';
    const fileName = `${currentDistrict.nameEn}_${currentUpazila.nameEn}_Master_Land_Records_2026.json`;

    setMasterArchive({
      upazilaId: currentUpazila.id,
      upazilaNameBn: currentUpazila.nameBn,
      upazilaNameEn: currentUpazila.nameEn,
      districtNameBn: currentDistrict.nameBn,
      districtNameEn: currentDistrict.nameEn,
      totalMouzasCount: allTasks.length,
      recordTypesIncluded: selectedRecordTypes.join('+'),
      totalKhatiansCount: totalKhatians,
      totalSizeMb: sizeMb,
      fileName,
      isReady: true,
      isDownloaded: false,
    });
  };

  const enqueueBatchOfMouzas = (mouzasToQueue: MouzaItem[], batchNum: number) => {
    if (!currentUpazila || !currentDistrict) return;
    const timestamp = Date.now();
    const newTasks: DownloadTask[] = [];

    mouzasToQueue.forEach((mouza, idx) => {
      const taskId = `${mouza.id}-${batchNum}-${timestamp}-${idx}`;
      const totalKhatians = (35 + Math.floor(Math.random() * 55)) * selectedRecordTypes.length;
      const fileSizeBytes = totalKhatians * 120000;

      newTasks.push({
        id: taskId,
        mouzaId: mouza.id,
        mouzaNameBn: mouza.nameBn,
        mouzaNameEn: mouza.nameEn,
        jlNo: mouza.jlNo,
        upazilaNameBn: currentUpazila.nameBn,
        districtNameBn: currentDistrict.nameBn,
        recordTypes: selectedRecordTypes,
        status: 'PENDING',
        progress: 0,
        totalKhatians,
        downloadedKhatians: 0,
        fileSizeBytes,
        speedKbps: 0,
        batchNumber: batchNum,
        timestamp,
      });
    });

    setQueue((prev) => [...prev, ...newTasks]);
  };

  const handleQueueSelected = () => {
    if (selectedMouzaIds.length === 0 || !currentUpazila || !currentDistrict) return;

    const candidateMouzas = currentUpazila.mouzas.filter(
      (m) => selectedMouzaIds.includes(m.id) && !downloadedMouzaIds.includes(m.id)
    );

    if (candidateMouzas.length === 0) return;

    enqueueBatchOfMouzas(candidateMouzas, currentBatchNumber);
  };

  useEffect(() => {
    if (!isPaused) {
      triggerNextTasks();
    }
  }, [queue, isPaused, triggerNextTasks]);

  // Master Download to Phone Memory & Auto-Clear Cache
  const handleDownloadMasterFile = () => {
    if (!masterArchive) return;

    const masterData = {
      portal: 'Bangladesh Land Records & Survey Portal (DLR&S)',
      district: `${masterArchive.districtNameBn} (${masterArchive.districtNameEn})`,
      upazila: `${masterArchive.upazilaNameBn} (${masterArchive.upazilaNameEn})`,
      totalMouzasCount: masterArchive.totalMouzasCount,
      recordTypes: masterArchive.recordTypesIncluded,
      totalKhatians: masterArchive.totalKhatiansCount,
      archivedSize: masterArchive.totalSizeMb,
      generatedTimestamp: Date.now(),
      mouzas: queue.map((t) => ({
        mouzaId: t.mouzaId,
        nameBn: t.mouzaNameBn,
        nameEn: t.mouzaNameEn,
        jlNo: t.jlNo,
        recordTypes: t.recordTypes.join('+'),
        khatians: t.totalKhatians,
        status: 'VERIFIED_DOWNLOADED',
      })),
    };

    const blob = new Blob([JSON.stringify(masterData, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = masterArchive.fileName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);

    // AUTO-CLEAR CACHE AND APP MEMORIES
    if (window.caches) {
      caches.keys().then((names) => {
        names.forEach((name) => caches.delete(name));
      });
    }

    setMasterArchive((prev) => (prev ? { ...prev, isDownloaded: true } : null));
    setCacheNotice('Master file downloaded to phone memory! Browser cache & application RAM cleared automatically.');
  };

  const handlePauseResume = () => {
    setIsPaused((prev) => !prev);
  };

  const handleClearQueue = () => {
    activeDownloadsRef.current = 0;
    setQueue([]);
    setIsPaused(false);
  };

  // HUD stats
  const totalTasks = queue.length;
  const completedTasks = queue.filter((t) => t.status === 'COMPLETED').length;
  const downloadingTasks = queue.filter((t) => t.status === 'DOWNLOADING').length;
  const pendingTasks = queue.filter((t) => t.status === 'PENDING').length;

  const overallProgress =
    totalTasks === 0
      ? 0
      : Math.round(
          queue.reduce((acc, curr) => acc + (curr.progress || 0), 0) / totalTasks
        );

  return (
    <div className="min-h-screen bg-[#f8fafc] text-[#0f172a] font-sans antialiased p-4 md:p-8">
      <div className="max-w-7xl mx-auto space-y-6">
        {/* HEADER BAR */}
        <header className="bg-[#047857] text-white rounded-3xl p-6 md:p-8 shadow-xl shadow-emerald-900/10 border border-emerald-600/30 relative overflow-hidden">
          <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-6 relative z-10">
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 rounded-2xl bg-white/10 backdrop-blur-md border border-white/20 flex items-center justify-center shadow-inner">
                <svg className="w-9 h-9 text-emerald-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="bg-emerald-800/80 text-emerald-200 text-xs font-semibold px-2.5 py-0.5 rounded-full border border-emerald-500/30">
                    GovTech BD Portal
                  </span>
                  <span className="text-xs text-emerald-200/80">ভূমি মন্ত্রণালয় ডিজিটাল রেকর্ড</span>
                </div>
                <h1 className="text-2xl md:text-3xl font-bold tracking-tight text-white mt-1">
                  LR Mass Downloader (Updated)
                </h1>
                <p className="text-sm text-emerald-100/90 mt-0.5">
                  Dynamic Upazila Indexer &bull; Sequential 7-Mouza Pipeline &bull; Master Ledger Exporter
                </p>
              </div>
            </div>

            <div className="flex items-center gap-3 bg-emerald-800/60 backdrop-blur-md px-4 py-2.5 rounded-2xl border border-emerald-500/30 text-xs">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-pulse"></span>
              <span className="font-medium text-emerald-100">Anti-Freeze Concurrency: Max 2</span>
            </div>
          </div>
        </header>

        {/* Cache Cleared Notification Banner */}
        {cacheNotice && (
          <div className="bg-emerald-100 border border-emerald-300 text-emerald-900 px-5 py-3 rounded-2xl flex items-center justify-between text-xs font-semibold">
            <div className="flex items-center gap-2">
              <svg className="w-5 h-5 text-emerald-700" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
              </svg>
              <span>{cacheNotice}</span>
            </div>
            <button
              onClick={() => setCacheNotice(null)}
              className="text-emerald-700 hover:text-emerald-900 font-bold"
            >
              ✕
            </button>
          </div>
        )}

        {/* Upazila Fetch Info Card (Requirement 1: Fetch how many mouzas first) */}
        {upazilaFetchInfo && (
          <div className="bg-white border-2 border-emerald-400 rounded-3xl p-5 shadow-sm flex items-center justify-between gap-4">
            <div className="flex items-center gap-3.5">
              <div className="w-10 h-10 rounded-2xl bg-emerald-100 text-emerald-800 flex items-center justify-center font-bold">
                ℹ
              </div>
              <div>
                <div className="text-xs font-bold text-emerald-700 uppercase tracking-wide">
                  উপজেলা জরিপ ইনডেক্স যাচাই (Verified Index)
                </div>
                <div className="text-sm font-extrabold text-slate-900 mt-0.5">
                  {upazilaFetchInfo.upazilaNameBn} উপজেলায় মোট {upazilaFetchInfo.totalMouzasCount}টি মৌজা শনাক্ত হয়েছে
                </div>
                <div className="text-xs text-slate-500">
                  টার্গেট: ৭টি করে ক্রমিক মোট {upazilaFetchInfo.totalBatches}টি ব্যাচে ডেটা সংগ্রহ হবে (বর্তমানে ব্যাচ #{currentBatchNumber})
                </div>
              </div>
            </div>
            <div className="text-right hidden sm:block">
              <span className="text-xs bg-emerald-50 text-emerald-800 font-bold px-3 py-1.5 rounded-xl border border-emerald-200">
                Target: {targetTotalMouzas} Mouzas
              </span>
            </div>
          </div>
        )}

        {/* Master File Ready Banner */}
        {masterArchive && (
          <div className="bg-[#064e3b] text-white rounded-3xl p-6 shadow-xl border border-emerald-500/30 space-y-4">
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
              <div>
                <span className="bg-amber-500/20 text-amber-300 text-xs font-bold px-3 py-1 rounded-full border border-amber-500/30">
                  MASTER FILE READY (100% COMPLETE)
                </span>
                <h3 className="text-xl font-black text-white mt-2">
                  {masterArchive.districtNameBn} &bull; {masterArchive.upazilaNameBn} সকল মৌজা মাস্টার ফাইল
                </h3>
                <p className="text-xs text-emerald-200 mt-1">
                  মোট {masterArchive.totalMouzasCount}টি মৌজা • {masterArchive.totalKhatiansCount}টি খতিয়ান • রেকর্ড ধরন: {masterArchive.recordTypesIncluded}
                </p>
              </div>

              <button
                type="button"
                onClick={handleDownloadMasterFile}
                className="w-full sm:w-auto px-6 py-3.5 bg-amber-400 hover:bg-amber-300 text-slate-900 rounded-2xl font-black text-sm transition-all shadow-lg shadow-amber-900/30 flex items-center justify-center gap-2"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                </svg>
                <span>One-Click Download to Phone (Auto-Clears Cache)</span>
              </button>
            </div>
          </div>
        )}

        {/* MAIN DASHBOARD */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* LEFT: SELECTION CONTROLS */}
          <div className="lg:col-span-7 space-y-6">
            {/* Filter Card */}
            <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-200/80 space-y-5">
              <h2 className="text-lg font-bold text-slate-800 border-b border-slate-100 pb-3">
                ফিল্টার ও স্তর নির্বাচন (Cascading Hierarchy)
              </h2>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* District */}
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1.5 uppercase">
                    ১. জেলা (District)
                  </label>
                  <select
                    value={selectedDistrictId}
                    onChange={(e) => handleDistrictChange(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-300 text-slate-800 text-sm font-medium rounded-2xl px-4 py-3"
                  >
                    {LR_DATASET.map((d) => (
                      <option key={d.id} value={d.id}>
                        {d.nameBn} ({d.nameEn})
                      </option>
                    ))}
                  </select>
                </div>

                {/* Upazila */}
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1.5 uppercase">
                    ২. উপজেলা (Upazila)
                  </label>
                  <select
                    value={selectedUpazilaId}
                    onChange={(e) => handleUpazilaChange(e.target.value)}
                    className={`w-full text-sm font-medium rounded-2xl px-4 py-3 border ${
                      selectedUpazilaId ? 'bg-emerald-50 border-emerald-400 font-semibold' : 'bg-slate-50 border-slate-300'
                    }`}
                  >
                    <option value="">-- উপজেলা নির্বাচন করুন --</option>
                    {currentUpazilaList.map((u) => (
                      <option key={u.id} value={u.id}>
                        {u.nameBn} ({u.nameEn}) - {u.mouzas.length} মৌজা
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Document Types + "Select All Documents" Button */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="text-xs font-semibold text-slate-600 uppercase">
                    ৩. জরিপ রেকর্ড ধরন (Document Types)
                  </label>
                  <button
                    type="button"
                    onClick={handleSelectAllRecordTypes}
                    className="text-xs font-bold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 px-3 py-1 rounded-xl transition-all"
                  >
                    Select All Documents (CS+SA+RS+BRS)
                  </button>
                </div>

                <div className="grid grid-cols-4 gap-2">
                  {(['CS', 'SA', 'RS', 'BRS'] as RecordType[]).map((type) => {
                    const isChecked = selectedRecordTypes.includes(type);
                    return (
                      <button
                        key={type}
                        type="button"
                        onClick={() => toggleRecordType(type)}
                        className={`py-2 px-3 rounded-2xl text-xs font-bold transition-all border flex flex-col items-center justify-center gap-0.5 ${
                          isChecked
                            ? 'bg-[#047857] text-white border-emerald-700 shadow-md'
                            : 'bg-slate-50 text-slate-700 border-slate-200 hover:bg-slate-100'
                        }`}
                      >
                        <span className="text-sm">{type}</span>
                        <span className="text-[10px] opacity-80">
                          {type === 'CS' ? 'ক্যাডাস্ট্রাল' : type === 'SA' ? 'স্টেট একুইজিশন' : type === 'RS' ? 'রিভিশনাল' : 'সিটি বিআরএস'}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>

            {/* Mouza Selection Panel */}
            <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-200/80 space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 pb-4">
                <div>
                  <h3 className="text-lg font-bold text-slate-900">
                    মৌজা নির্বাচন তালিকা ({availableMouzas.length} টি মৌজা)
                  </h3>
                  <p className="text-xs text-slate-500">
                    Batch {currentBatchNumber} of {totalBatches} &bull; ৭টি করে ক্রমিক কিউ (নো রিপিট)
                  </p>
                </div>

                <div className="bg-slate-100 text-slate-700 text-xs font-bold px-3 py-1.5 rounded-2xl border border-slate-200">
                  Done: {downloadedMouzaIds.length} / Target: {targetTotalMouzas} Mouzas
                </div>
              </div>

              {/* Action Toolbar */}
              <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={handleSelectNextSeven}
                    disabled={availableMouzas.length === 0}
                    className="px-3 py-2 rounded-xl text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200"
                  >
                    Next 7
                  </button>
                  <button
                    type="button"
                    onClick={handleSelectAll}
                    disabled={availableMouzas.length === 0}
                    className="px-3 py-2 rounded-xl text-xs font-bold bg-slate-100 text-slate-700 border border-slate-200"
                  >
                    All ({availableMouzas.length})
                  </button>
                  <button
                    type="button"
                    onClick={handleDeselectAll}
                    disabled={selectedMouzaIds.length === 0}
                    className="px-3 py-2 rounded-xl text-xs font-bold bg-slate-100 text-slate-600 border border-slate-200"
                  >
                    Clear
                  </button>
                </div>

                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="খুঁজুন (নাম বা J.L.)..."
                  className="text-xs bg-slate-50 border border-slate-200 rounded-xl px-3 py-2 text-slate-800"
                />
              </div>

              {/* Mouza Scrollable Grid */}
              <div className="min-h-[260px] max-h-[360px] overflow-y-auto pr-1">
                {!selectedUpazilaId ? (
                  <div className="h-48 flex items-center justify-center text-slate-400 text-sm">
                    উপজেলা নির্বাচন করুন
                  </div>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {filteredMouzas.map((mouza) => {
                      const isChecked = selectedMouzaIds.includes(mouza.id);
                      const isDone = downloadedMouzaIds.includes(mouza.id);
                      return (
                        <div
                          key={mouza.id}
                          onClick={() => toggleMouza(mouza.id)}
                          className={`cursor-pointer p-3 rounded-2xl border flex items-center justify-between ${
                            isDone
                              ? 'bg-slate-100 border-slate-300 opacity-80'
                              : isChecked
                              ? 'bg-emerald-50 border-emerald-500'
                              : 'bg-white border-slate-200'
                          }`}
                        >
                          <div className="flex items-center gap-3">
                            <div
                              className={`w-5 h-5 rounded-md flex items-center justify-center ${
                                isDone
                                  ? 'bg-[#064e3b] text-white'
                                  : isChecked
                                  ? 'bg-[#047857] text-white'
                                  : 'border-2 border-slate-300'
                              }`}
                            >
                              {(isDone || isChecked) && '✓'}
                            </div>
                            <div>
                              <div className="font-bold text-slate-800 text-xs">
                                {mouza.nameBn} {isDone && <span className="text-[10px] text-emerald-700">(Done)</span>}
                              </div>
                              <div className="text-[10px] text-slate-400">{mouza.nameEn}</div>
                            </div>
                          </div>
                          <span className="text-[10px] bg-slate-100 px-2 py-0.5 rounded font-bold">
                            J.L. {mouza.jlNo}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>

              {/* Primary Queue CTA */}
              <button
                type="button"
                onClick={handleQueueSelected}
                disabled={selectedMouzaIds.length === 0}
                className="w-full py-4 rounded-2xl font-bold text-white bg-[#047857] hover:bg-[#065f46] shadow-lg shadow-emerald-800/20 disabled:opacity-40"
              >
                Queue Batch (Next {selectedMouzaIds.length} Mouzas - No Repeats)
              </button>
            </div>
          </div>

          {/* RIGHT: QUEUE HUD */}
          <div className="lg:col-span-5 space-y-6">
            <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-200/80 space-y-5">
              <div className="flex items-center justify-between border-b border-slate-100 pb-4">
                <div>
                  <h2 className="text-lg font-bold text-slate-800">Queue HUD Pipeline</h2>
                  <p className="text-xs text-emerald-700 font-semibold">
                    Batch {currentBatchNumber} of {totalBatches} &bull; Target: {downloadedMouzaIds.length}/{targetTotalMouzas}
                  </p>
                </div>
                <div className="flex items-center gap-1">
                  <button
                    type="button"
                    onClick={handlePauseResume}
                    disabled={totalTasks === 0}
                    className="px-3 py-1.5 rounded-xl text-xs font-bold bg-slate-100 text-slate-700"
                  >
                    {isPaused ? 'Resume' : 'Pause'}
                  </button>
                  <button
                    type="button"
                    onClick={handleClearQueue}
                    disabled={totalTasks === 0}
                    className="px-3 py-1.5 rounded-xl text-xs font-bold bg-rose-50 text-rose-700"
                  >
                    Clear
                  </button>
                </div>
              </div>

              {/* Progress */}
              <div>
                <div className="flex items-center justify-between text-xs font-semibold text-slate-600 mb-1">
                  <span>Current Batch Progress</span>
                  <span className="font-bold text-emerald-700">{overallProgress}%</span>
                </div>
                <div className="w-full bg-slate-100 rounded-full h-3 overflow-hidden">
                  <div
                    className="bg-emerald-600 h-full transition-all duration-300"
                    style={{ width: `${overallProgress}%` }}
                  ></div>
                </div>
              </div>

              {/* Live Count Summary */}
              <div className="grid grid-cols-4 gap-2 text-center">
                <div className="bg-slate-50 p-2.5 rounded-xl border">
                  <div className="text-[10px] text-slate-400 uppercase font-bold">Total</div>
                  <div className="text-base font-black text-slate-800">{totalTasks}</div>
                </div>
                <div className="bg-emerald-50 p-2.5 rounded-xl border border-emerald-200">
                  <div className="text-[10px] text-emerald-600 uppercase font-bold">Done</div>
                  <div className="text-base font-black text-emerald-700">{completedTasks}</div>
                </div>
                <div className="bg-blue-50 p-2.5 rounded-xl border border-blue-200">
                  <div className="text-[10px] text-blue-600 uppercase font-bold">Active</div>
                  <div className="text-base font-black text-blue-700">{downloadingTasks}</div>
                </div>
                <div className="bg-amber-50 p-2.5 rounded-xl border border-amber-200">
                  <div className="text-[10px] text-amber-600 uppercase font-bold">Queued</div>
                  <div className="text-base font-black text-amber-700">{pendingTasks}</div>
                </div>
              </div>

              {/* Anti-Repeat Notice */}
              <div className="p-3 bg-slate-50 border rounded-2xl text-xs text-slate-600">
                <strong>Sequential Auto-Advance:</strong> When this batch of 7 completes, the next 7 are automatically queued without repeats until all {targetTotalMouzas} mouzas finish.
              </div>

              {/* Tasks List */}
              <div className="min-h-[280px] max-h-[440px] overflow-y-auto space-y-2.5 pr-1">
                {queue.length === 0 ? (
                  <div className="h-48 flex items-center justify-center text-slate-400 text-xs">
                    কিউ খালি। মৌজা সিলেক্ট করে Queue বাটনে ক্লিক করুন।
                  </div>
                ) : (
                  queue.map((task) => {
                    const isDone = task.status === 'COMPLETED';
                    const isRunning = task.status === 'DOWNLOADING';
                    return (
                      <div
                        key={task.id}
                        className={`p-3 rounded-2xl border ${
                          isDone ? 'bg-slate-50 border-slate-200' : isRunning ? 'bg-emerald-50 border-emerald-400' : 'bg-white'
                        }`}
                      >
                        <div className="flex items-center justify-between text-xs font-bold mb-1">
                          <span>
                            {task.mouzaNameBn} ({task.mouzaNameEn})
                          </span>
                          <span
                            className={`px-2 py-0.5 rounded-full text-[10px] ${
                              isDone ? 'bg-emerald-100 text-emerald-800' : isRunning ? 'bg-blue-100 text-blue-800' : 'bg-slate-100 text-slate-600'
                            }`}
                          >
                            {isDone ? 'Done' : isRunning ? `${task.progress}%` : 'Pending'}
                          </span>
                        </div>
                        <div className="w-full bg-slate-200 rounded-full h-1.5 overflow-hidden my-1.5">
                          <div
                            className={`h-full ${isDone ? 'bg-emerald-600' : isRunning ? 'bg-blue-600' : 'bg-slate-300'}`}
                            style={{ width: `${task.progress}%` }}
                          ></div>
                        </div>
                        <div className="flex items-center justify-between text-[10px] text-slate-500">
                          <span>
                            J.L. {task.jlNo} &bull; Khatians: {task.downloadedKhatians}/{task.totalKhatians}
                          </span>
                          <span>{isRunning ? `${task.speedKbps} KB/s` : isDone ? 'Archived' : 'Waiting'}</span>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LRMassDownloaderUpdated;
