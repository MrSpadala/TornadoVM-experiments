#pragma OPENCL EXTENSION cl_khr_fp64 : enable  
#pragma OPENCL EXTENSION cl_khr_int64_base_atomics : enable  
__kernel void matrixMultiplication(__global uchar *_heap_base, ulong _frame_base, __constant uchar *_constant_region, __local uchar *_local_region, __global int *_atomics)
{
  long l_45, l_44, l_11, l_43, l_10, l_9, l_21, l_20, l_19; 
  int i_42, i_8, i_17, i_49, i_50, i_15, i_47, i_16, i_48, i_24, i_29, i_30, i_27, i_33, i_34, i_5, i_6, i_3, i_35, i_4; 
  float f_14, f_13, f_18, f_23, f_26, f_25, f_28, f_32, f_31, f_36, f_38, f_37, f_40, f_7, f_39, f_41; 
  ulong ul_46, ul_12, ul_22, ul_1, ul_0; 

  __global ulong *_frame = (__global ulong *) &_heap_base[_frame_base];


  // BLOCK 0
  ul_0  =  (ulong) _frame[3];
  ul_1  =  (ulong) _frame[7];
  __private float ul_2[59];
  i_3  =  get_global_id(0);
  // BLOCK 1 MERGES [0 17 ]
  i_4  =  i_3;
  for(;i_4 < 8132;)  {
    // BLOCK 2
    i_5  =  i_4 + 1;
    i_6  =  i_4 + 60;
    // BLOCK 3 MERGES [2 4 ]
    f_7  =  0.0F;
    i_8  =  i_5;
    for(;i_8 < i_6;)    {
      // BLOCK 4
      l_9  =  (long) i_8;
      l_10  =  l_9 << 2;
      l_11  =  l_10 + 24L;
      ul_12  =  ul_0 + l_11;
      f_13  =  *((__global float *) ul_12);
      f_14  =  f_7 + f_13;
      i_15  =  i_8 + 1;
      f_7  =  f_14;
      i_8  =  i_15;
    }  // B4
    // BLOCK 5
    i_16  =  -1 - i_4;
    // BLOCK 6 MERGES [5 7 ]
    f_18  =  f_7 / 60.0F;
    i_17  =  i_5;
    for(;i_17 < i_6;)    {
      // BLOCK 7
      l_19  =  (long) i_17;
      l_20  =  l_19 << 2;
      l_21  =  l_20 + 24L;
      ul_22  =  ul_0 + l_21;
      f_23  =  *((__global float *) ul_22);
      i_24  =  i_16 + i_17;
      f_25  =  f_23 - f_18;
      f_26  =  fabs(f_25);
      ul_2[i_24]  =  f_26;
      i_27  =  i_17 + 1;
      i_17  =  i_27;
    }  // B7
    // BLOCK 8
    // BLOCK 9 MERGES [8 10 ]
    f_28  =  0.0F;
    i_29  =  i_5;
    for(;i_29 < i_6;)    {
      // BLOCK 10
      i_30  =  i_29 + i_16;
      f_31  =  ul_2[i_30];
      f_32  =  f_28 + f_31;
      i_33  =  i_29 + 1;
      f_28  =  f_32;
      i_29  =  i_33;
    }  // B10
    // BLOCK 11
    // BLOCK 12 MERGES [11 16 ]
    i_34  =  i_5;
    for(;i_34 < i_6;)    {
      // BLOCK 13
      i_35  =  i_34 + i_16;
      f_36  =  ul_2[i_35];
      f_37  =  f_28 / 60.0F;
      f_38  =  f_37 * 1.4826F;
      f_39  =  f_38 * 2.0F;
      f_40  =  f_36 - f_18;
      f_41  =  fabs(f_40);
      i_42  =  i_34 + 1;
      l_43  =  (long) i_4;
      l_44  =  l_43 << 2;
      l_45  =  l_44 + 24L;
      ul_46  =  ul_1 + l_45;
      i_47  =  isless(f_39, f_41);
      if(i_47 == 1)
      {
        // BLOCK 14
        *((__global float *) ul_46)  =  1.0F;
      }  // B14
      else
      {
        // BLOCK 15
        *((__global float *) ul_46)  =  0.0F;
      }  // B15
      // BLOCK 16 MERGES [14 15 ]
      i_48  =  i_42;
      i_34  =  i_48;
    }  // B16
    // BLOCK 17
    i_49  =  get_global_size(0);
    i_50  =  i_49 + i_4;
    i_4  =  i_50;
  }  // B17
  // BLOCK 18
  return;
}  //  kernel
